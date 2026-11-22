"""Génère app/src/main/assets/nutrition/foods.json à partir de la table Ciqual 2020 et de selection.csv.

Usage (depuis la racine du dépôt, jamais depuis le dossier du fichier téléchargé) :
    python -I tools/ciqual/build_foods.py <chemin/vers/Table Ciqual 2020_FR_2020 07 07.xls>

Dépendance : xlrd (le fichier officiel est au format XLS).
"""
import csv
import json
import re
import sys
import unicodedata
from pathlib import Path

import xlrd

ROOT = Path(__file__).resolve().parents[2]
SELECTION = Path(__file__).resolve().parent / "selection.csv"
OUTPUT = ROOT / "app" / "src" / "main" / "assets" / "nutrition" / "foods.json"
SOURCE = "Anses, table de composition nutritionnelle des aliments Ciqual 2020"
KINDS = {"NORMAL", "SEASONING", "FRYING"}
AISLES = ["Fruits et légumes", "Crèmerie", "Boucherie et poisson", "Épicerie salée",
          "Épicerie sucrée", "Surgelés", "Boissons", "Autres"]
# Rayon par sous-groupe Ciqual (prioritaire), puis par groupe ; « Autres » sinon.
AISLE_BY_SUBGROUP = {
    "légumineuses": "Épicerie salée",
    "fruits à coque et graines oléagineuses": "Épicerie sucrée",
    "beurres": "Crèmerie",
    "margarines": "Crèmerie",
    "huiles et graisses végétales": "Épicerie salée",
    "autres matières grasses": "Épicerie salée",
    "œufs": "Crèmerie",
    "plats composés": "Autres",
}
AISLE_BY_GROUP = {
    "fruits, légumes, légumineuses et oléagineux": "Fruits et légumes",
    "produits laitiers et assimilés": "Crèmerie",
    "viandes, œufs, poissons et assimilés": "Boucherie et poisson",
    "produits céréaliers": "Épicerie salée",
    "aides culinaires et ingrédients divers": "Épicerie salée",
    "matières grasses": "Épicerie salée",
    "produits sucrés": "Épicerie sucrée",
    "eaux et autres boissons": "Boissons",
    "glaces et sorbets": "Surgelés",
}

COL_CODE = "alim_code"
COL_NAME = "alim_nom_fr"
COL_GROUP = "alim_grp_nom_fr"
COL_SUBGROUP = "alim_ssgrp_nom_fr"
COL_KCAL = "(kcal/100 g)"  # « Energie, Règlement UE N° 1169/2011 (kcal/100 g) » en priorité
COL_PROT = "Protéines, N x facteur de Jones (g/100 g)"
COL_CARB = "Glucides (g/100 g)"
COL_FAT = "Lipides (g/100 g)"
COL_FIBRE = "Fibres alimentaires (g/100 g)"
COL_ALCOHOL = "Alcool (g/100 g)"


def normalize(text: str) -> str:
    """Même normalisation que Normalize.kt (minuscules, sans accents, pluriels simples)."""
    s = text.lower().replace("œ", "oe").replace("æ", "ae").replace("’", "'").replace("`", "'")
    s = "".join(c for c in unicodedata.normalize("NFD", s) if unicodedata.category(c) != "Mn")
    words = re.findall(r"[a-z0-9%]+", s)
    out = []
    for w in words:
        if len(w) > 3 and w.endswith("aux"):
            w = w[:-3] + "al"
        elif len(w) > 2 and (w.endswith("s") or w.endswith("x")):
            w = w[:-1]
        out.append(w)
    return " ".join(out)


def number(raw) -> float | None:
    """Valeur Ciqual → nombre ; « traces » et « < x » → 0 ; « - » ou vide → None."""
    if isinstance(raw, (int, float)):
        return float(raw)
    s = str(raw).strip().replace(",", ".")
    if s in ("", "-"):
        return None
    if s.lower().startswith("traces") or s.startswith("<"):
        return 0.0
    return float(s)


def aisle_for(forced: str, group: str, subgroup: str) -> str:
    """Rayon : colonne « aisle » de selection.csv si remplie, sinon déduit du sous-groupe puis du groupe Ciqual."""
    if forced:
        if forced not in AISLES:
            raise SystemExit(f"Rayon inconnu : {forced} (attendu : {', '.join(AISLES)})")
        return forced
    return AISLE_BY_SUBGROUP.get(subgroup.strip()) or AISLE_BY_GROUP.get(group.strip()) or "Autres"


def find_col(header, needle, exact=False):
    for i, h in enumerate(header):
        if (h == needle) if exact else (needle in h):
            return i
    raise SystemExit(f"Colonne introuvable : {needle}")


def main(xls_path: str) -> None:
    sheet = xlrd.open_workbook(xls_path).sheet_by_index(0)
    header = [str(h) for h in sheet.row_values(0)]
    c_code = find_col(header, COL_CODE, exact=True)
    c_name = find_col(header, COL_NAME, exact=True)
    c_group = find_col(header, COL_GROUP, exact=True)
    c_subgroup = find_col(header, COL_SUBGROUP, exact=True)
    kcal_cols = [i for i, h in enumerate(header) if h.endswith(COL_KCAL)]  # 1169/2011 d'abord, puis Jones
    c_prot, c_carb, c_fat, c_fibre, c_alc = (find_col(header, n) for n in (COL_PROT, COL_CARB, COL_FAT, COL_FIBRE, COL_ALCOHOL))

    rows = {}
    for r in range(1, sheet.nrows):
        v = sheet.row_values(r)
        rows[str(int(v[c_code]))] = v

    foods, seen_alias, warnings, synthetic = [], {}, [], 0
    with open(SELECTION, encoding="utf-8") as f:
        for line in csv.DictReader(f, delimiter=";"):
            code, kind = line["code"].strip(), line["kind"].strip()
            aliases = [a.strip() for a in line["aliases"].split("|") if a.strip()]
            if kind not in KINDS:
                raise SystemExit(f"kind invalide pour {code} : {kind}")
            for a in aliases:
                key = normalize(a)
                if key in seen_alias and seen_alias[key] != code:
                    raise SystemExit(f"Alias en double après normalisation : « {a} » ({seen_alias[key]} et {code})")
                seen_alias[key] = code
            piece = float(line["pieceG"]) if line["pieceG"].strip() else None
            density = float(line["density"]) if line["density"].strip() else None
            forced_aisle = (line.get("aisle") or "").strip()
            if code == "-":
                if kind != "SEASONING":
                    raise SystemExit(f"Aliment sans code Ciqual autorisé seulement pour SEASONING : {aliases[0]}")
                synthetic += 1
                foods.append({"code": f"X{synthetic}", "name": aliases[0].capitalize(), "aliases": aliases,
                              "kcal": 0.0, "protein": 0.0, "carbs": 0.0, "fat": 0.0,
                              "pieceG": piece, "density": density, "kind": kind,
                              "aisle": aisle_for(forced_aisle, "", "")})
                continue
            if code not in rows:
                raise SystemExit(f"Code Ciqual introuvable : {code}")
            v = rows[code]
            prot = number(v[c_prot]) or 0.0
            carb = number(v[c_carb]) or 0.0
            fat = number(v[c_fat]) or 0.0
            kcal = next((k for k in (number(v[i]) for i in kcal_cols) if k is not None), None)
            if kcal is None:
                # Coefficients du règlement UE 1169/2011 : protéines et glucides 4, lipides 9, fibres 2, alcool 7.
                fibre = number(v[c_fibre]) or 0.0
                alcohol = number(v[c_alc]) or 0.0
                kcal = round(4 * prot + 4 * carb + 9 * fat + 2 * fibre + 7 * alcohol, 1)
                warnings.append(f"{code} {v[c_name]} : énergie calculée = {kcal}")
            foods.append({"code": code, "name": str(v[c_name]).strip(), "aliases": aliases,
                          "kcal": round(kcal, 1), "protein": round(prot, 2), "carbs": round(carb, 2),
                          "fat": round(fat, 2), "pieceG": piece, "density": density, "kind": kind,
                          "aisle": aisle_for(forced_aisle, str(v[c_group]), str(v[c_subgroup]))})

    codes = [f["code"] for f in foods]
    if len(codes) != len(set(codes)):
        raise SystemExit("Code en double dans selection.csv")
    OUTPUT.parent.mkdir(parents=True, exist_ok=True)
    OUTPUT.write_text(json.dumps({"source": SOURCE, "version": 1, "foods": foods},
                                 ensure_ascii=False, indent=1), encoding="utf-8")
    print("\n".join(warnings))
    print(f"{len(foods)} aliments écrits dans {OUTPUT} ({len(warnings)} énergies calculées)")


if __name__ == "__main__":
    if len(sys.argv) != 2:
        raise SystemExit(__doc__)
    main(sys.argv[1])
