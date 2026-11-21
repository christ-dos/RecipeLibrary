package com.example.recipelibrary.nutrition

import com.example.recipelibrary.data.Recipe
import com.example.recipelibrary.data.nutrition.NutritionCalculator
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

/**
 * SC-001 : sur 20 recettes de référence aux quantités précises, au moins 18 donnent une valeur
 * par part à ±15 % du calcul fait à la main.
 *
 * Valeur attendue = somme (grammes fixés à la main d'après le texte × kcal/100 g de la table
 * Ciqual 2020) ÷ parts, calculée indépendamment de l'analyseur. Détail en commentaire.
 */
class ReferenceRecipesTest {

    private class Ref(val title: String, val category: String, val servings: Int, val expectedPerServing: Double, val lines: List<String>)

    private val refs = listOf(
        // farine 250 × 356 + œufs 220 × 140 + lait 515 × 46,4 + beurre 50 × 753 = 1813 kcal
        Ref("Pâte à crêpes", "Desserts", 4, 453.0, listOf("250 g de farine", "4 œufs", "50 cl de lait", "50 g de beurre", "1 pincée de sel")),
        // pâte brisée 230 × 377 + lardons 200 × 275 + œufs 165 × 140 + crème 200 × 304 + lait 103 × 46,4 = 2304
        Ref("Quiche lorraine", "Tartes et quiches", 6, 384.0, listOf("1 pâte brisée", "200 g de lardons fumés", "3 œufs", "20 cl de crème fraîche", "10 cl de lait", "sel, poivre", "1 pincée de muscade")),
        // yaourt 125 × 45,5 + sucre 200 × 399,2 + farine 250 × 356 + œufs 165 × 140 + huile 92 × 901 + levure 11 × 146,2 = 2821
        Ref("Gâteau au yaourt", "Gâteaux", 8, 353.0, listOf("1 yaourt nature", "200 g de sucre", "250 g de farine", "3 œufs", "10 cl d'huile", "1 sachet de levure chimique")),
        // spaghetti 400 × 336 + bœuf 400 × 209 + oignon 100 × 38,4 + ail 5 × 111 + coulis 500 × 49,3 + huile d'olive 27,6 × 900 = 2719
        Ref("Spaghetti bolognaise", "Pâtes et riz", 4, 680.0, listOf("400 g de spaghetti", "400 g de bœuf haché", "1 oignon", "1 gousse d'ail", "500 g de coulis de tomate", "2 c. à soupe d'huile d'olive", "sel, poivre")),
        // tomates 480 × 19,3 + thon 200 × 111 + œufs 220 × 140 + olives 100 × 332 + poivron 150 × 26 + huile 41,4 × 900 + vinaigre 15 × 22,6 = 1370
        Ref("Salade niçoise", "Salades", 4, 342.0, listOf("4 tomates", "200 g de thon au naturel", "4 œufs", "100 g d'olives noires", "1 poivron vert", "3 c. à soupe d'huile d'olive", "1 c. à soupe de vinaigre")),
        // potiron 1000 × 19,9 + pommes de terre 300 × 80,5 + oignon 100 × 38,4 + eau 1000 × 0 + crème 100 × 297 = 776
        Ref("Soupe de potiron", "Soupes", 4, 194.0, listOf("1 kg de potiron", "2 pommes de terre", "1 oignon", "1 l d'eau", "10 cl de crème liquide", "sel")),
        // chocolat 200 × 591 + œufs 330 × 140 + sucre 30 × 399,2 = 1764
        Ref("Mousse au chocolat", "Desserts", 6, 294.0, listOf("200 g de chocolat noir", "6 œufs", "30 g de sucre", "1 pincée de sel")),
        // pâte brisée 230 × 377 + pommes 600 × 57,1 + sucre 50 × 399,2 + beurre 30 × 753 = 1635
        Ref("Tarte aux pommes", "Tartes et quiches", 8, 204.0, listOf("1 pâte brisée", "4 pommes", "50 g de sucre", "30 g de beurre")),
        // poulet 600 × 110 + riz 300 × 352 + lait de coco 400 × 188 + oignon 100 × 38,4 + huile 13,8 × 901 = 2631
        Ref("Poulet au curry", "Plats principaux", 4, 658.0, listOf("600 g de blanc de poulet", "300 g de riz", "40 cl de lait de coco", "1 oignon", "2 c. à soupe de curry", "1 c. à soupe d'huile")),
        // farine 200 × 356 + beurre 100 × 753 + sucre roux 100 × 389,7 + œuf 55 × 140 + chocolat 150 × 591 + levure 5,5 × 146,2 = 2826
        Ref("Cookies", "Biscuits et petits gâteaux", 20, 141.0, listOf("200 g de farine", "100 g de beurre", "100 g de sucre roux", "1 œuf", "150 g de pépites de chocolat", "1/2 sachet de levure chimique")),
        // pommes de terre 1500 × 80,5 + crème 500 × 297 + lait 257,5 × 46,4 + ail 10 × 111 + emmental 100 × 373 = 3196
        Ref("Gratin dauphinois", "Accompagnements", 6, 533.0, listOf("1,5 kg de pommes de terre", "50 cl de crème liquide", "25 cl de lait", "2 gousses d'ail", "100 g de gruyère râpé", "sel")),
        // bananes 240 × 90,5 + lait 257,5 × 46,4 + yaourt 125 × 45,5 + miel 21 × 329 = 463
        Ref("Smoothie banane", "Boissons", 2, 231.0, listOf("2 bananes", "25 cl de lait", "1 yaourt nature", "1 c. à soupe de miel")),
        // pain de mie 200 × 278 + jambon 180 × 125 + emmental 100 × 373 + beurre 30 × 753 = 1380
        Ref("Croque-monsieur", "Sandwichs et wraps", 4, 345.0, listOf("8 tranches de pain de mie", "4 tranches de jambon", "100 g d'emmental", "30 g de beurre")),
        // huile d'olive 82,8 × 900 + vinaigre 30 × 22,6 + moutarde 5,25 × 152 = 760
        Ref("Vinaigrette", "Sauces", 8, 95.0, listOf("6 c. à soupe d'huile d'olive", "2 c. à soupe de vinaigre", "1 c. à café de moutarde", "sel, poivre")),
        // farine 250 × 356 + œufs 110 × 140 + lait 309 × 46,4 + sucre 30 × 399,2 + levure 11 × 146,2 + beurre 40 × 753 = 1624
        Ref("Pancakes", "Petits-déjeuners", 4, 406.0, listOf("250 g de farine", "2 œufs", "30 cl de lait", "30 g de sucre", "1 sachet de levure chimique", "40 g de beurre fondu")),
        // pois chiches 400 + tahin 33 + jus de citron 46,35 + huile d'olive 41,4 × 900 + ail 5 × 111 = 1078
        Ref("Houmous", "Apéritifs", 6, 180.0, listOf("400 g de pois chiches", "2 c. à soupe de tahin", "3 c. à soupe de jus de citron", "3 c. à soupe d'huile d'olive", "1 gousse d'ail")),
        // aubergines 600 + courgettes 600 × 16,5 + poivrons 300 × 36,6 + tomates 480 × 19,3 + oignons 200 × 38,4 + huile 69 × 900 = 1123
        Ref("Ratatouille", "Accompagnements", 6, 187.0, listOf("2 aubergines", "3 courgettes", "2 poivrons rouges", "4 tomates", "2 oignons", "5 c. à soupe d'huile d'olive", "thym, laurier")),
        // chocolat 200 × 591 + beurre 150 × 753 + sucre 150 × 399,2 + œufs 220 × 140 + farine 50 × 356 = 3396
        Ref("Fondant au chocolat", "Gâteaux", 8, 425.0, listOf("200 g de chocolat noir", "150 g de beurre", "150 g de sucre", "4 œufs", "50 g de farine")),
        // riz 150 × 352 + lait 1030 × 46,4 + sucre 100 × 399,2 = 1405
        Ref("Riz au lait", "Desserts", 6, 234.0, listOf("150 g de riz rond", "1 l de lait", "100 g de sucre", "1 gousse de vanille")),
        // œufs 330 × 140 + lait 30,9 × 46,4 + beurre 10 × 753 + comté 50 × 418 = 761
        Ref("Omelette au comté", "Plats principaux", 2, 380.0, listOf("6 œufs", "2 c. à soupe de lait", "10 g de beurre", "50 g de comté râpé"))
    )

    @Test
    fun atLeast18Of20Within15Percent() {
        val calc = NutritionCalculator(TestFoods.table)
        val report = StringBuilder()
        var ok = 0
        for (r in refs) {
            val e = calc.estimate(
                Recipe(id = r.title, title = r.title, category = r.category, ingredients = r.lines, servings = r.servings, servingsSource = Recipe.SERVINGS_USER)
            )
            val got = e.perServing?.kcal
            val pass = got != null && abs(got - r.expectedPerServing) <= 0.15 * r.expectedPerServing
            if (pass) ok++
            report.append("${if (pass) "ok" else "KO"} ${r.title} : attendu ${r.expectedPerServing}, obtenu $got ; non comptés ${e.ignored.map { it.parsed.line }}\n")
        }
        println(report)
        assertTrue("$ok / ${refs.size} à ±15 %\n$report", ok >= 18)
    }

    @Test
    fun crepesAreCloseToHandComputation() {
        // Test indépendant de la story 1 : 4 parts, valeur par part à ±15 % du calcul à la main
        val e = NutritionCalculator(TestFoods.table).estimate(
            Recipe(
                id = "c", title = "Pâte à crêpes", servings = 4, servingsSource = Recipe.SERVINGS_USER,
                ingredients = listOf("250 g de farine", "4 œufs", "50 cl de lait", "50 g de beurre")
            )
        )
        assertTrue("obtenu ${e.perServing}", abs(e.perServing!!.kcal - 453.0) <= 0.15 * 453.0)
    }
}
