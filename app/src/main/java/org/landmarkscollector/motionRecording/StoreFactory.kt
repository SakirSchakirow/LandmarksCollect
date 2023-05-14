package org.landmarkscollector.motionRecording

import android.content.Context
import org.landmarkscollector.domain.repository.ExportRepositoryImpl
import org.landmarkscollector.domain.repository.csv.CsvFileCreator
import org.landmarkscollector.domain.repository.csv.DataConverterCSV
import org.landmarkscollector.domain.repository.file.AndroidInternalStorageFileWriter
import org.tensorflow.lite.Interpreter
import vivid.money.elmslie.core.store.ElmStore
import java.io.FileInputStream
import java.io.IOException
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

internal fun storeFactory(context: Context) = ElmStore(
    initialState = State.NoCameraPermitted,
    reducer = Reducer(fileCreator = CsvFileCreator(context)),
    actor = Actor(
        interpreter = { Interpreter(loadModelFile(context)) },
        exportRepository = ExportRepositoryImpl(
            fileWriter = AndroidInternalStorageFileWriter(context),
            dataConverter = DataConverterCSV()
        )
    )
)

@Throws(IOException::class)
private fun loadModelFile(context: Context): MappedByteBuffer {
    val MODEL_ASSETS_PATH = "gesturesModel.tflite"
    val assetFileDescriptor = context.assets.openFd(MODEL_ASSETS_PATH)
    return FileInputStream(assetFileDescriptor.fileDescriptor)
        .channel
        .map(
            FileChannel.MapMode.READ_ONLY,
            assetFileDescriptor.startOffset,
            assetFileDescriptor.declaredLength
        )
}
