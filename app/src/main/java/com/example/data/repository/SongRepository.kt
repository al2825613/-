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
            val songDataList = listOf(
                Pair("حلف القمر", "Halaf El Qamar"),
                Pair("طبيب جراح", "Tabeeb Garrah"),
                Pair("كلام الناس", "Kalam El Nas"),
                Pair("سلف ودين", "Salaf We Deen"),
                Pair("الهوى سلطان", "El Hawa Sultan"),
                Pair("يوم الوداع", "Youm El Wadaa"),
                Pair("روح الروح", "Rouh El Rouh"),
                Pair("لسه الدنيا بخير", "Lissa El Donya Bkheir"),
                Pair("صابر وراضي", "Saber We Rady"),
                Pair("صياد الطيور", "Sayyad El Toyour"),
                Pair("لو نويت", "Law Nawayt"),
                Pair("قدك المياس", "Qaddokal Mayyas"),
                Pair("خسرت كل الناس", "Khesert Kol El Nas"),
                Pair("حد ينسى قلبه", "Had Yensa Qalbo"),
                Pair("بستان تسلملي", "Bostan Teslamly"),
                Pair("الحب الكبير", "El Hob El Kabeer"),
                Pair("الحب الأولاني", "El Hob El Awalany"),
                Pair("ارمي الشبك", "Irmi El Shabak"),
                Pair("سلمتك بيد الله", "Sallamtak Biyad Allah"),
                Pair("قلبك طيب", "Qalbak Tayyeb"),
                Pair("شيء غريب", "Shee Ghareeb"),
                Pair("بستني باليوم واليومين", "Bastanny Bel Youm"),
                Pair("ليلة وداعنا", "Laylat Wadaana"),
                Pair("حظ يا بخت", "Hazz Ya Bakht"),
                Pair("حنين", "Haneen"),
                Pair("حارمنا من أنسك", "Haremna Men Ansak"),
                Pair("شكراً", "Shokran"),
                Pair("بنفكر في الناس", "Bnefaker Fel Nas"),
                Pair("ذكريات", "Zekrayat"),
                Pair("عيون القلب", "Oyoun El Qalb"),
                Pair("جرحونا", "Garahona"),
                Pair("الحب شاطر", "El Hob Shater"),
                Pair("زمن العجايب", "Zaman El Ajaweb"),
                Pair("لو يواعدني", "Law Yowaadny"),
                Pair("يا عيني ع الآه", "Ya Einy Al Ah"),
                Pair("أصعب فراق", "Asab Foraq"),
                Pair("فرحة رجوعك يا غالي", "Farhet Rogoouak"),
                Pair("دول مش حبايب", "Dol Mesh Habayeb"),
                Pair("ليل العاشقين", "Layl El Ashiqeen"),
                Pair("الذهب يا حبيبي", "El Dahab Ya Habibi"),
                Pair("الحب كدة", "El Hob Keda"),
                Pair("يا بياعين الهوى", "Ya Bayaeen El Hawa"),
                Pair("يا ريتني", "Ya Raytany"),
                Pair("مريم", "Maryam"),
                Pair("حبيبي كده", "Habibi Keda"),
                Pair("سكت الكلام", "Sakat El Kalam"),
                Pair("ملكة جمال الروح", "Malekat Gamal El Rouh"),
                Pair("صاحي الليل", "Sahi El Layl"),
                Pair("ياه على الزمن", "Yah Al Zaman"),
                Pair("ترغلي يا ترغلي", "Tareghly Ya Tareghly"),
                Pair("مسافري الغربة", "Msafery El Ghorba"),
                Pair("حكاية غرام", "Hikayat Gharam"),
                Pair("عيون غزلان", "Oyoun Ghazlan"),
                Pair("يا ريت كل القلوب", "Ya Rayt Kol El Qoloub"),
                Pair("القلوب عند بعضها", "El Qoloub End Badha"),
                Pair("على جبينك مكتوب", "Ala Gbeenak Maktoub"),
                Pair("دار الزمان", "Dar El Zaman"),
                Pair("مغرم يا ليل", "Moghram Ya Layl"),
                Pair("روح يا نسمة", "Rouh Ya Nasma"),
                Pair("الأيام بتعدي", "El Ayyam Bteaddy"),
                Pair("عاشق ومغرم", "Ashiq We Moghram"),
                Pair("الغالي", "El Ghaly"),
                Pair("يا حبيبي قولي", "Ya Habibi Qooly"),
                Pair("جرحونا برضاهم", "Garahona Berdahom"),
                Pair("سلمت قلبي ليك", "Sallamt Qalby Leek"),
                Pair("مسافر", "Musafer"),
                Pair("سيبهم يقولوا", "Seebhom Yaqoolo"),
                Pair("الصبر طيب", "El Sabr Tayyeb"),
                Pair("من هنا ورايح", "Men Hena We Rayeh"),
                Pair("بتعاتبني على كلمة", "Btatebny Ala Kelma"),
                Pair("يا أغلى من عيني", "Ya Aghla Men Einy"),
                Pair("حيرة العاشقين", "Hayrat El Ashiqeen"),
                Pair("طربيات وسوفية", "Tarabiyat Wassoufya"),
                Pair("حبايبنا فين", "Habayebna Fein"),
                Pair("كلنا مجروحين", "Kolona Magrouheen"),
                Pair("دمعة حزن", "Damaat Hozn"),
                Pair("قلب المحب دليله", "Qalb El Moheb"),
                Pair("يا نسمة الصيف", "Ya Nasmat El Sayf"),
                Pair("طاروا الطيور", "Tarou El Toyour"),
                Pair("لا تروح وتغيب", "La Trooh We Tgheeb"),
                Pair("يا عيني على الصبر", "Ya Einy Al Sabr"),
                Pair("بكتبلك بالدموع", "Baktoblak Bel Domooa"),
                Pair("كلامك يا حبيبي مسك", "Kalamak Musk"),
                Pair("صرخة وجع", "Sarkhat Wajaa"),
                Pair("عدي وبس", "Addy We Bas"),
                Pair("عاشق عيونك", "Ashiq Oyounak"),
                Pair("الهوى غلاب", "El Hawa Ghallab"),
                Pair("ليالي الحب راحت فين", "Layaly El Hob"),
                Pair("يا زمان الآهات", "Ya Zaman El Ahat"),
                Pair("طال السفر يا غائب", "Tal El Safar"),
                Pair("دموع الفراق حارة", "Domooa El Foraq"),
                Pair("سلطان الهوى", "Sultan El Hawa"),
                Pair("يا بياع الورد", "Ya Bayaa El Ward"),
                Pair("سهرت الليل", "Sahart El Layl"),
                Pair("الحبايب", "El Habayeb"),
                Pair("لو كل عاشق", "Law Kol Ashiq"),
                Pair("أنا مسافر يا أمي", "Ana Musafer Ya Omy"),
                Pair("شيبون", "Sheeboun"),
                Pair("رحل البطل", "Rahal El Batal"),
                Pair("الزمن دوار", "Al Zaman Dawwar")
            )

            // Let's build exactly 100 songs dynamically with different SoundHelix URLs
            val defaultSongs = songDataList.mapIndexed { index, pair ->
                val songIndex = (index % 16) + 1 // SoundHelix provides multiple songs up to 16
                val durationText = when (index % 4) {
                    0 -> "05:12"
                    1 -> "06:40"
                    2 -> "05:54"
                    else -> "07:05"
                }
                
                Song(
                    id = "wassouf_song_${index + 1}",
                    title = pair.second,
                    arabicTitle = pair.first,
                    description = "من درر طرب أبو وديع الخالدة والمميزة - أغنية رقم ${index + 1}.",
                    durationText = durationText,
                    lyrics = """أغنية ${pair.first} الخالدة بصوت أبو وديع

يا حبيبي الهوى طرب وألحان
وسلطنة طربية مع أبو وديع الإمبراطور
يا ريت كل القلوب تسمع وتندرج في عالم السلطنة واللحن الأصيل.
حبيبي يا ملهم وعمري الجميل فراقك وجع ملوش بديل.""",
                    remoteUrl = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-$songIndex.mp3"
                )
            }

            for (song in defaultSongs) {
                val existing = songDao.getSongById(song.id)
                if (existing == null) {
                    songDao.insertSongs(listOf(song))
                }
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
