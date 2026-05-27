package com.example.data.repository

import android.content.Context
import android.util.Log
import com.example.data.database.SongDao
import com.example.data.model.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL

class SongRepository(
    private val context: Context,
    private val songDao: SongDao
) {
    val allSongs: Flow<List<Song>> = songDao.getAllSongs()
    val downloadedSongs: Flow<List<Song>> = songDao.getDownloadedSongs()
    val favoriteSongs: Flow<List<Song>> = songDao.getFavoriteSongs()

    suspend fun initializeDefaultSongsIfNeeded() {
        withContext(Dispatchers.IO) {
            val count = songDao.getAllSongs().first().size
            if (count == 0) {
                val songs = listOf(
                    Song(
                        id = "halaf_el_qamar",
                        title = "Halaf El Qamar",
                        arabicTitle = "حلف القمر",
                        description = "من روائع جورج وسوف الكلاسيكية التي أطلقته في سماء الفن.",
                        durationText = "05:12",
                        lyrics = """حلف القمر يمين الله يا حبنا
أغلى من عيوننا ونور قلبنا
يا حبيبي يلا نعيش في عيون الليل
يلا نعيش في نور القمر
ونقول للدنيا دي كلها يا حبنا
أغلى من عيوننا ونور قلبنا

الله على حبنا الله عليه
من كل العيون حنخاف عليه
مهما جرى مهما كان
إنت حبيبي لآخر الزمان

حلف القمر يمين الله يا حبنا
أغلى من عيوننا ونور قلبنا""",
                        remoteUrl = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3"
                    ),
                    Song(
                        id = "tabeeb_garrah",
                        title = "Tabeeb Garrah",
                        arabicTitle = "طبيب جراح",
                        description = "أغنية حزينة معبرة ولحن ذهبي يسلط الضوء على آلام الجراح.",
                        durationText = "06:40",
                        lyrics = """طبيب جراح قلوب الناس أداويها
ياما جراح سهرت الليل إداريها
وأنا اللي بيا ياما جراح ما أقسى لياليها
طبيب جراح قلوب الناس أداويها

يا ريت كل القلوب تنسى جراحها
وتعيش مرتاحة الفرحة تملى لياليها
وأنا اللي بيا ياما جراح ما أقسى لياليها

لو كل عاشق جرح يتداوى قلبه بساعة
كنت شلت جروحي كلها لدنيا بيا شجاعة
لكن جرح الهوى ملوش نهاية ودايم الصراع""",
                        remoteUrl = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-2.mp3"
                    ),
                    Song(
                        id = "kalam_el_nas",
                        title = "Kalam El Nas",
                        arabicTitle = "كلام الناس",
                        description = "أحد أكثر الأغاني شهرة وجماهيرية، تناقش نظرة المجتمع وقوتها.",
                        durationText = "05:54",
                        lyrics = """كلام الناس لا بيقدم ولا بيأخر
كلام الناس ملامة وغيره مش أكثر
وليه بنلتم بلوم الناس؟
وليه بنهتم بكلام الناس؟
حبيبي أنا وإنت وبس
كفاية علينا حب وهمس

سهرنا الشوق ونسينا الخوف
وقابلنا في دنيتنا ظروف
وعمر كلامهم ما يفرقنا
ولا كلمة عذول حتعوقنا...

كلام الناس لا بيقدم ولا بيأخر
كلام الناس ملامة وغيره مش أكثر""",
                        remoteUrl = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-3.mp3"
                    ),
                    Song(
                        id = "salaf_we_deen",
                        title = "Salaf We Deen",
                        arabicTitle = "سلف ودين",
                        description = "أغنية درامية رائعة تتحدث عن العدل والجزاء والزمن بكبرياء.",
                        durationText = "07:05",
                        lyrics = """سلف ودين والفرق شاسع بين الزمن
والدنيا دي بتعطي الدروس بأغلى ثمن
بكرة تدور الأيام عليك
واللي عملته يرجع ليك
وحتدفع الثمن غالي
وتندم ع اللي فات يا غالي

سلف ودين...
كنت فاكر إنك أقوى من الزمان
وإن قلبك مش حيعرف الهوان
لكن الزمن دوّار
والله على المظلوم ستّار...""",
                        remoteUrl = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-4.mp3"
                    ),
                    Song(
                        id = "el_hawa_sultan",
                        title = "El Hawa Sultan",
                        arabicTitle = "الهوى سلطان",
                        description = "لقب بها جورج وسوف (سلطان الطرب)، أغنية الطرب الأصيل.",
                        durationText = "06:15",
                        lyrics = """الهوى سلطان يا حبيبي الهوى سلطان
مين يرضى يحب يا عمري وهو هيمان
الحب عذاب وسهر ونواح
لكن بيه الروح بترتاح
يا حبيبي سلمتك عمري
وانت في قلبي وعيني سلطان

الهوى سلطان...
سهرنا ليالي الهوى الجميل
وعرفنا في حبه طعم المستحيل
حبك خلّى دنيتي ورد وأنوار
يا ملاكي يا أغلى الأزهار""",
                        remoteUrl = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-5.mp3"
                    )
                )
                songDao.insertSongs(songs)
            }
        }
    }

    suspend fun toggleFavorite(songId: String, currentStatus: Boolean) {
        songDao.updateFavoriteStatus(songId, !currentStatus)
    }

    suspend fun deleteDownloadedFile(songId: String) {
        withContext(Dispatchers.IO) {
            val song = songDao.getSongById(songId)
            if (song != null && song.localFilePath != null) {
                val file = File(song.localFilePath)
                if (file.exists()) {
                    file.delete()
                }
                songDao.updateDownloadStatus(songId, "NOT_DOWNLOADED", 0, null)
            }
        }
    }

    suspend fun downloadSong(songId: String, remoteUrl: String) {
        withContext(Dispatchers.IO) {
            try {
                songDao.updateDownloadStatus(songId, "DOWNLOADING", 5, null)
                
                val url = URL(remoteUrl)
                val connection = url.openConnection() as HttpURLConnection
                connection.connectTimeout = 15000
                connection.readTimeout = 15000
                connection.connect()

                if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                    songDao.updateDownloadStatus(songId, "FAILED", 0, null)
                    return@withContext
                }

                val fileLength = connection.contentLength
                val input: InputStream = connection.inputStream
                
                val localFile = File(context.filesDir, "wassouf_$songId.mp3")
                val output = FileOutputStream(localFile)

                val data = ByteArray(4096)
                var total: Long = 0
                var count: Int
                var lastProgressUpdate = 0

                while (input.read(data).also { count = it } != -1) {
                    total += count
                    if (fileLength > 0) {
                        val progress = ((total * 100) / fileLength).toInt()
                        if (progress - lastProgressUpdate >= 5) {
                            lastProgressUpdate = progress
                            songDao.updateDownloadStatus(songId, "DOWNLOADING", progress, null)
                        }
                    }
                    output.write(data, 0, count)
                }

                output.flush()
                output.close()
                input.close()

                songDao.updateDownloadStatus(songId, "COMPLETED", 100, localFile.absolutePath)
                Log.d("SongRepository", "Successfully downloaded song $songId to ${localFile.absolutePath}")
            } catch (e: Exception) {
                Log.e("SongRepository", "Error downloading song $songId", e)
                songDao.updateDownloadStatus(songId, "FAILED", 0, null)
            }
        }
    }
}
