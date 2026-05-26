package com.example.data.repository

import com.example.data.database.SongDao
import com.example.data.model.Song
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class SongRepository(private val songDao: SongDao) {

    val allSongs: Flow<List<Song>> = songDao.getAllSongs()
    val favoriteSongs: Flow<List<Song>> = songDao.getFavoriteSongs()

    fun searchSongs(query: String): Flow<List<Song>> {
        return songDao.searchSongs("%$query%")
    }

    fun getSongsByCategory(category: String): Flow<List<Song>> {
        return songDao.getSongsByCategory(category)
    }

    suspend fun toggleFavorite(songId: Int, currentStatus: Boolean) {
        songDao.updateFavoriteStatus(songId, !currentStatus)
    }

    suspend fun incrementPlayCount(songId: Int) {
        songDao.incrementPlayCount(songId)
    }

    suspend fun checkAndPrepopulate() {
        if (songDao.getSongsCount() == 0) {
            val defaultSongs = listOf(
                Song(
                    title = "كلام الناس",
                    englishTitle = "Kalam El Nass",
                    album = "كلام الناس",
                    year = "1994",
                    lyrics = """كلام الناس لا بيقدم ولا يأخر
كلام الناس ملامة وغيره مش أكتر

ورأسك فوق راسي يا ملاكي
بلاش نخاف من الدنيا وعيونها
وتنسى الحب وعهوده وظنونها

أنا وياك يا حبيبي أنا وياك
ولا همّك كلام الناس

لو قصرنا يوم بالحيرة والظنون
لو عاندنا قلب وجرحنا العيون
أهو كله بيمر ويفوت
والشوق في قلوبنا ما بيموت
شوقنا يا حبيبي ما بيموت""",
                    duration = 310,
                    audioUrl = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3", // high quality real streaming fallback
                    imageUrl = "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=400",
                    category = "طرب",
                    isFavorite = true
                ),
                Song(
                    title = "طبيب جراح",
                    englishTitle = "Tabeeb Jarrah",
                    album = "طبيب جراح",
                    year = "1999",
                    lyrics = """طبيب جراح.. قلوب الناس أداويها
وياما جراح.. بلمساتي أشفيها

وأنا اللي بيا جراح.. أطباء الكون ما تشفيها

ياللي بتشتكي من الهوى وتقول حبيبي جرحني وسابني
الجرح ده ياما نوى يتعبني ياما عذبني
لكني صابر طيِّب وراضي
وبقول لكل الناس فرحي ده عادي
وجرحي أنا.. قلبي أنا.. جرحي وعذابي وحدي يا عين""",
                    duration = 345,
                    audioUrl = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-2.mp3",
                    imageUrl = "https://images.unsplash.com/photo-1514525253161-7a46d19cd819?w=400",
                    category = "شجن",
                    isFavorite = false
                ),
                Song(
                    title = "صابر وراضي",
                    englishTitle = "Saber W Rady",
                    album = "سلف ودين",
                    year = "2003",
                    lyrics = """صابر وراضي ع اللي جرالي
وبقول ده نصيب ومكتوب ع الجبين

يا قلبي اصبر ده العمر والي
بين الشقا والتعب والآه والحنين

سنين راحت وسنين جاية
والدمعة باينة جوة عينيا
بس الأمل لسه في إيديه
وحبيبي ويايا في كل حين""",
                    duration = 295,
                    audioUrl = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-3.mp3",
                    imageUrl = "https://images.unsplash.com/photo-1470225620780-dba8ba36b745?w=400",
                    category = "كلاسيكيات",
                    isFavorite = false
                ),
                Song(
                    title = "الهوى سلطان",
                    englishTitle = "El Hawa Sultan",
                    album = "الهوى سلطان",
                    year = "1984",
                    lyrics = """الهوى سلطان يا عاشقين
الهوى سلطان.. هنيئاً لكم يا أهل الهوى

أنا قلبي غدا في بحر الهوى غريق
أفتش عن طبيب يدلني ع الطريق
يا ريت قلبي ما حب ولا مال
ولا شاف الغرام ولا عرف الدلال
لكنه مكتوب.. وصار الهوى سلطان""",
                    duration = 278,
                    audioUrl = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-4.mp3",
                    imageUrl = "https://images.unsplash.com/photo-1506157786151-b8491531f063?w=400",
                    category = "طرب",
                    isFavorite = false
                ),
                Song(
                    title = "سلف ودين",
                    englishTitle = "Salaf W Dein",
                    album = "سلف ودين",
                    year = "2003",
                    lyrics = """سلف ودين.. الدنيا دي سلف ودين
واللي عملته فيا بكرة تلاقيه في عينيك

كنت المانح وأنت الآخذ واليوم دار الزمان
وين الهنا والعهد اللي عشته ليا بالأمان
ظلمت قلبي وسامحتك ياما
وقلت بكرة يحس بالندامة
لكن طريق الغدر مالوش سلامة
بكرة تشوف الويل ودينك سلف ودين""",
                    duration = 360,
                    audioUrl = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-5.mp3",
                    imageUrl = "https://images.unsplash.com/photo-1459749411175-04bf5292ceea?w=400",
                    category = "شجن",
                    isFavorite = false
                ),
                Song(
                    title = "خسرت كل الناس",
                    englishTitle = "Khesert Kol El Nass",
                    album = "روائع وسنجلات",
                    year = "2005",
                    lyrics = """خسرت كل الناس علشان أراضيك
ومشيت وياك في سكة ضاع عمري فيها

كنت بحبك حب جنون وبهتف بيك
وقلت الدنيا دي ما تسوى إلا عينيك
لكنك بعت الشوق والغرام في ثانية
وسبتني حاير وحدي في وسط الدنيا
خسرت كل الناس وأهو ضعت من بين إيديك""",
                    duration = 320,
                    audioUrl = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-6.mp3",
                    imageUrl = "https://images.unsplash.com/photo-1501386761578-eac5c94b800a?w=400",
                    category = "شجن",
                    isFavorite = false
                ),
                Song(
                    title = "حلف القمر",
                    englishTitle = "Haleft El Qamar",
                    album = "الهوى سلطان",
                    year = "1985",
                    lyrics = """حلف القمر يمين الله يا حلوة لولا عيوني
ما كان الجمال نال شرف الوجود

حلف الغصن برقة دلالك ولين قوامك
أنه في روعتك محال في الدنيا يجود

أنت الجمال كله وأنت النور الصافي
يا ليتها عيوني تكون غطاك الضافي
وصار قلبي يدق في حبك بعود""",
                    duration = 240,
                    audioUrl = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-7.mp3",
                    imageUrl = "https://images.unsplash.com/photo-1498038432885-c6f3f1b912ee?w=400",
                    category = "كلاسيكيات",
                    isFavorite = false
                ),
                Song(
                    title = "لسه الدنيا بخير",
                    englishTitle = "Lissa El Donya Bkheir",
                    album = "لسه الدنيا بخير",
                    year = "1998",
                    lyrics = """لسه الدنيا بخير يا حبيبي لسه الدنيا بخير
لسه قلوب الناس مليانة حب وشوق وبخير

لو ضاق بيك الزمان ويوم فارقك الخليل
افتكر أن الفجر طالع دايماً بعد الليل الطويل
وسيبك من الأحزان وعيش الشوق الغزير
الدنيا لسه جميلة وفيها فرح كتير""",
                    duration = 312,
                    audioUrl = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-8.mp3",
                    imageUrl = "https://images.unsplash.com/photo-1511379938547-c1f69419868d?w=400",
                    category = "كلاسيكيات",
                    isFavorite = false
                ),
                Song(
                    title = "لو نويت",
                    englishTitle = "Law Nawayt",
                    album = "روائع وسنجلات",
                    year = "1997",
                    lyrics = """لو نويت تنسى اللي فات والضماير بيننا ماتت
فريّحني وقولهالي بلاش تخلّي الشك يطوّل

أنا لو نويت هقدر أبيع حتى لو حبك صانع ربيعي
لكني بحبك حب يخلّي غدرك عندي محال
حبيبي ارحم دمعة عيني وصفي النية وكفاية دلال""",
                    duration = 290,
                    audioUrl = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-9.mp3",
                    imageUrl = "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=400",
                    category = "طرب",
                    isFavorite = false
                ),
                Song(
                    title = "حبيبي كده",
                    englishTitle = "Habibi Keda",
                    album = "طبيب جراح",
                    year = "2001",
                    lyrics = """حبيبي كده.. وهوايا كده.. وراضية أنا بيه لو حتى عذاب
روحي معاه.. وهوايا هواه.. وفي حبه العمر يهون لو غاب

أنا بحبه برغم اللي قاسيته وياه ومعاه
برغم البعد وبرغم الجرح وروحي فداه
يا ناس قولوا له إن هواه مالي الكون وأنا بهواه""",
                    duration = 310,
                    audioUrl = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-10.mp3",
                    imageUrl = "https://images.unsplash.com/photo-1514525253161-7a46d19cd819?w=400",
                    category = "طرب",
                    isFavorite = false
                ),
                Song(
                    title = "روحي يا نسمة",
                    englishTitle = "Rouhi Ya Nasma",
                    album = "روائع وسنجلات",
                    year = "1988",
                    lyrics = """روحي يا نسمة روحي للي بحبه قولي له
أنا ع البعد صابر وراضي وباقي على عهده ومواويله

قولي له إنه في بالي وفي خيالي دايماً حبيب قلبي
وروحي بتروح مع طيفه الدافي كل ليلة من غلبي
طال البعاد يا حبيبي والقلب داب من لهيبه وعذابه""",
                    duration = 350,
                    audioUrl = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-11.mp3",
                    imageUrl = "https://images.unsplash.com/photo-1470225620780-dba8ba36b745?w=400",
                    category = "طرب",
                    isFavorite = false
                ),
                Song(
                    title = "حد ينسى قلبه",
                    englishTitle = "Had Yensa Qalbo",
                    album = "روائع وسنجلات",
                    year = "2008",
                    lyrics = """حد ينسى قلبه يا حبيبي ويمشي في سكة ضياع
أنا وعيونك عشنا المحبة وكان الهوى من غير وداع

إزاي تفارقني وتسيب الشوق جوة ضلوعي نار
وبتقولي خلاص الحب ولى وصار في خبر كان وصار
يا ريتك ما جيت ولا شفت الهوى ومكتوبنا صار مرار""",
                    duration = 330,
                    audioUrl = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-12.mp3",
                    imageUrl = "https://images.unsplash.com/photo-1506157786151-b8491531f063?w=400",
                    category = "شجن",
                    isFavorite = false
                ),
                Song(
                    title = "يوم الوداع",
                    englishTitle = "Youm El Wada'",
                    album = "روائع وسنجلات",
                    year = "1996",
                    lyrics = """يوم الوداع انحرمت النوم وعشت بغرابة ومظلوم
والصبر فارق جراحي وسال فجأة المكتوب

كان عهدي ويا طيفك باقٍ والقلب ع الوجع مصدوم
يا ريت عيونك تحن ثانية وتشفي غليل داب هموم
الدمعة باينة في عينيا والزمان عليّ قاسي يلوم""",
                    duration = 305,
                    audioUrl = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-13.mp3",
                    imageUrl = "https://images.unsplash.com/photo-1459749411175-04bf5292ceea?w=400",
                    category = "شجن",
                    isFavorite = false
                ),
                Song(
                    title = "شي غريب",
                    englishTitle = "Shee Ghareeb",
                    album = "روائع وسنجلات",
                    year = "1990",
                    lyrics = """شي غريب والله غريب الحب في العالم مسافات
ناس تندم وناس تفرح وناس تعيش ع الذكريات

وأنا اللي عشت الغرام كله سهر وليل وعتاب
مكتوب لي أشقى في بحر عيونك وأدوق طعم العذاب
يا أهل الهوى دبروني صرت غيماً يروي التراب""",
                    duration = 340,
                    audioUrl = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-14.mp3",
                    imageUrl = "https://images.unsplash.com/photo-1501386761578-eac5c94b800a?w=400",
                    category = "كلاسيكيات",
                    isFavorite = false
                ),
                Song(
                    title = "بتعتب عليّ",
                    englishTitle = "Bte'teb Alaya",
                    album = "روائع وسنجلات",
                    year = "1990",
                    lyrics = """بتعتب عليّ البعد وجرح الشوق والظنون
وأنت اللي بعت الحب وهجرت أوفى العيون

ده أنا روحي كانت في إيدك وبشوف الدنيا بيك
ودلوقتي راجع تلومني والدمعة تملأ عينيك
خلاص يا مسافر وداع وما تلوم غير نفسيك""",
                    duration = 285,
                    audioUrl = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-15.mp3",
                    imageUrl = "https://images.unsplash.com/photo-1498038432885-c6f3f1b912ee?w=400",
                    category = "كلاسيكيات",
                    isFavorite = false
                ),
                Song(
                    title = "انت غيرهم",
                    englishTitle = "Enta Gheyrohom",
                    album = "روائع وسنجلات",
                    year = "2002",
                    lyrics = """أنت غيرهم يا حبيبي أنت في عيني ملاك
العمر يحلى ويزيد بهاء وبصوتك المداوي وطيب هواك

ما تفتكر كلام الواشي لو قيل في غرامنا عتاب
أنت البداية وأنت النهاية في دفتر الأحباب
ولا يوم تغيب الفرحة طول ما روحي وياك""",
                    duration = 315,
                    audioUrl = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3",
                    imageUrl = "https://images.unsplash.com/photo-1511379938547-c1f69419868d?w=400",
                    category = "طرب",
                    isFavorite = false
                ),
                Song(
                    title = "قلب عاشق دليله",
                    englishTitle = "Qalb Asheq Daleelo",
                    album = "روائع وسنجلات",
                    year = "1991",
                    lyrics = """قلب عاشق وعاشق دليله ما يرجع في كلامه ويغش
الحب عهد ووفا صافي ما يعرف يوم غدر وغش

يا ليت القلوب تكون مخلصة مثل قلبي الحزين
عايش ع الوداد وأمال اللقى طول هالسنين
أبو وديع بينده بالهوى ويهدي الشوق للساكنين""",
                    duration = 325,
                    audioUrl = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-2.mp3",
                    imageUrl = "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=400",
                    category = "كلاسيكيات",
                    isFavorite = false
                ),
                Song(
                    title = "شكراً",
                    englishTitle = "Shokran",
                    album = "روائع وسنجلات",
                    year = "2009",
                    lyrics = """شكراً لأنك حطمت عهد الهوى والوفا والجمال
وسبتني بحيرتي وأشواقي عايش ع خيوط الخيال

أنا مش زعلان يا مسافر وبتمنالك أطيب نصيب
بكرة الزمن يعلمك إن الوفا غالي وصعب يغيب
روحي مسامحة جراحك وبقول لكل الناس فرحي نصيب""",
                    duration = 310,
                    audioUrl = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-3.mp3",
                    imageUrl = "https://images.unsplash.com/photo-1470225620780-dba8ba36b745?w=400",
                    category = "شجن",
                    isFavorite = false
                )
            )
            songDao.insertSongs(defaultSongs)
        }
    }
}
