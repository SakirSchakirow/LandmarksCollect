package org.landmarkscollector.motionRecording

import android.content.Context
import org.landmarkscollector.domain.repository.ExportRepositoryImpl
import org.landmarkscollector.domain.repository.csv.CsvFileCreator
import org.landmarkscollector.domain.repository.csv.DataConverterCSV
import org.landmarkscollector.domain.repository.file.AndroidInternalStorageFileWriter
import vivid.money.elmslie.core.store.ElmStore

internal fun storeFactory(context: Context) = ElmStore(
    initialState = State.NoCameraPermitted,
    reducer = Reducer(fileCreator = CsvFileCreator(context)),
    actor = Actor(
        exportRepository = ExportRepositoryImpl(
            fileWriter = AndroidInternalStorageFileWriter(context),
            dataConverter = DataConverterCSV()
        )
    )
)
