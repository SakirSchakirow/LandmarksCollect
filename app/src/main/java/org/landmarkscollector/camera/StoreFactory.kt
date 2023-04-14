package org.landmarkscollector.camera

import android.content.Context
import org.landmarkscollector.domain.repository.ExportRepositoryImpl
import org.landmarkscollector.domain.repository.csv.CsvFileCreator
import org.landmarkscollector.domain.repository.csv.DataConverterCSV
import org.landmarkscollector.domain.repository.file.AndroidInternalStorageFileWriter
import vivid.money.elmslie.core.store.ElmStore

fun storeFactory(context: Context) = ElmStore(
    initialState = State.Steady.WaitingForDirectoryAndGesture(),
    reducer = Reducer(fileCreator = CsvFileCreator(context)),
    actor = Actor(
        exportRepository = ExportRepositoryImpl(
            fileWriter = AndroidInternalStorageFileWriter(context),
            dataConverter = DataConverterCSV()
        )
    )
)
