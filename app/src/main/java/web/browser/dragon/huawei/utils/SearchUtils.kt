@file:Suppress("ControlFlowWithEmptyBody", "UNREACHABLE_CODE")

package web.browser.dragon.huawei.utils

import android.content.Context
import com.google.gson.Gson
import web.browser.dragon.huawei.R
import web.browser.dragon.huawei.model.SearchEngine
import java.util.*


fun saveSelectedSearchEngine(context: Context, searchEngine: SearchEngine?) {
    context.getSharedPreferences(Constants.Search.SEARCH, Context.MODE_PRIVATE)
        .edit()
        .putString(Constants.Search.SEARCH_SELECTED, Gson().toJson(searchEngine))
        .commit()
}

fun getSelectedSearchEngine(context: Context?): SearchEngine? {
    val json = context
        ?.getSharedPreferences(Constants.Search.SEARCH, Context.MODE_PRIVATE)
        ?.getString(Constants.Search.SEARCH_SELECTED, null)
    return if(!json.isNullOrEmpty()) {
        json.let { Gson().fromJson(json, SearchEngine::class.java) }
    }
    else {
        null
    }
}
        //var countryName1 = getSearchEngines
//        val country = countryName
//       if (country == "US") || (country == "FR") || (country == "DE") || (country == "GB") || (country == "CA"){
//           return
//
//
//            }
//              else(country != "US") || (country != "FR")|| (country != "DE") || (country != "GB") || (country != "CA"){
//
//        }
//    }



//        }else ((country == "US") || (country == "FR") || (country == "DE") || (country == "GB") || (country == "CA")){
//            fun getSearchEngines(context: android.content.Context): ArrayList<SearchEngine2> {
//                val arr = kotlin.collections.arrayListOf<web.browser.dragon.huawei.model.SearchEngine2>()
//                arr.add(
//                    web.browser.dragon.huawei.model.SearchEngine(
//                        0,
//                        context.getString(web.browser.dragon.huawei.R.string.google),
//                        "https://www.google.com/search?q="
//                    )
//                )
//
//                arr.add(
//                    web.browser.dragon.huawei.model.SearchEngine(
//                        1,
//                        context.getString(web.browser.dragon.huawei.R.string.yandex),
//                        "https://yandex.ru/search/?&text="
//                    )
//                )
//
//                arr.add(
//                    web.browser.dragon.huawei.model.SearchEngine(
//                        2,
//                        context.getString(web.browser.dragon.huawei.R.string.bing),
//                        //     if ( = )
//                        //"https://www.bing.com/search?q="
//                        "https://t.supersimplesearch1.com/searchm?q="
//                        //
//                    )
//                )
//
//                arr.add(
//                    web.browser.dragon.huawei.model.SearchEngine(
//                        3,
//                        context.getString(web.browser.dragon.huawei.R.string.duck_duck_go),
//                        "https://duckduckgo.com/?q="
//                    )
//                )
//
//                return arr
//
//
//        }
//
//        }
//
//



//fun saveSearchEngines(context: Context, arr: ArrayList<SearchEngine>) {
//    val sharedPref = context.getSharedPreferences(Constants.Search.SEARCH, Context.MODE_PRIVATE) ?: return
//
//    if(arr.find { it.id == getSelectedSearchEngine(context)?.id } != null) {
//        saveSelectedSearchEngine(context, arr.find { it.id == getSelectedSearchEngine(context)?.id })
//    }
//
//    with(sharedPref.edit()) {
//        putString(Constants.Search.SEARCH_ENGINES, Gson().toJson(arr))
//        commit()
//    }
//}
//
//fun getSearchEngines(context: Context): ArrayList<SearchEngine> {
//    val sharedPref = context.getSharedPreferences(Constants.Search.SEARCH, Context.MODE_PRIVATE)
//    val objectJson = sharedPref.getString(Constants.Search.SEARCH_ENGINES, "")
//    val objectType = object : TypeToken<ArrayList<SearchEngine>>() {}.type
//    return Gson().fromJson(objectJson, objectType) ?: arrayListOf()
//}
fun getCountryCode(countryName:String): String? = Locale.getISOCountries().find {
       Locale("", it).displayCountry == countryName
    }



fun getSearchEngines(context: Context): ArrayList<SearchEngine> {

    val countryName = getCountryCode("Canada")





    val arr = arrayListOf<SearchEngine>()


    arr.add(
        SearchEngine(
            0,
            context.getString(R.string.google),
            "https://www.google.com/search?q="
        )
    )

    arr.add(
        SearchEngine(
            1,
            context.getString(R.string.yandex),
            "https://yandex.ru/search/?&text="
        )
    )


    if ((countryName == "US") ||
        (countryName == "FR") ||
        (countryName == "CA") ||
        (countryName == "DE") ||
        (countryName == "GB") ||
        (countryName == "AU")

    )


     {
        arr.add(
            SearchEngine(
                2,
                context.getString(R.string.bing),
                "https://t.supersimplesearch1.com/searchm?q="
//            "https://www.google.com/search?q="



            )

        )
    } else {
        arr.add(
            SearchEngine(
                2,
                context.getString(R.string.bing),
              "https://www.bing.com/search?q="
            //"https://www.youtube.com"


            )
        )
    }


    arr.add(
        SearchEngine(
            3,
            context.getString(R.string.duck_duck_go),
            "https://duckduckgo.com/?q="
        )
    )
    return arr
}







