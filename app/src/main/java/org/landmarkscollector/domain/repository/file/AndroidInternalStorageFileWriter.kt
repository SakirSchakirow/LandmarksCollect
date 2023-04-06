package org.landmarkscollector.domain.repository.file

import android.content.Context
import android.net.Uri
import org.landmarkscollector.data.Resource
import java.io.File
import java.io.IOException

class AndroidInternalStorageFileWriter(
    private val context: Context
) : FileWriter {


    override suspend fun writeFile(
        byteArray: ByteArray,
        futureCsvFile: Uri
    ): Resource<Uri> {
        val os = context.contentResolver.openOutputStream(futureCsvFile)!!
        return try {
            os.write(byteArray)
            os.flush()
            Resource.Success(futureCsvFile)
        } catch (e: IOException) {
            //Log error
            Resource.Error(errorMessage = e.localizedMessage ?: "unknown error")
        } finally {
            os.close()
        }
    }

    private fun deleteDir(dir: File?): Boolean {
        return if (dir != null && dir.isDirectory) {
            val children = dir.list()
            if (children != null) {
                for (i in children.indices) {
                    val success = deleteDir(File(dir, children[i]))
                    if (!success) {
                        return false
                    }
                }
            }
            dir.delete()
        } else if (dir != null && dir.isFile) {
            dir.delete()
        } else {
            false
        }
    }
}
