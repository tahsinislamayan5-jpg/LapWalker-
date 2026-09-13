package com.takwa.lapwalker.di

import androidx.room.Room
import com.takwa.lapwalker.data.local.datastore.SettingsDataStore
import com.takwa.lapwalker.data.local.db.AppDatabase
import com.takwa.lapwalker.data.repository.SettingsRepositoryImpl
import com.takwa.lapwalker.data.repository.WorkoutRepositoryImpl
import com.takwa.lapwalker.domain.repository.SettingsRepository
import com.takwa.lapwalker.domain.repository.WorkoutRepository
import com.takwa.lapwalker.domain.usecase.CalculatePaceUseCase
import com.takwa.lapwalker.domain.usecase.ClearAllWorkoutsUseCase
import com.takwa.lapwalker.domain.usecase.DeleteWorkoutUseCase
import com.takwa.lapwalker.domain.usecase.GetSettingsUseCase
import com.takwa.lapwalker.domain.usecase.GetWorkoutsUseCase
import com.takwa.lapwalker.domain.usecase.RecordLapUseCase
import com.takwa.lapwalker.domain.usecase.SaveSettingsUseCase
import com.takwa.lapwalker.domain.usecase.SaveWorkoutUseCase
import com.takwa.lapwalker.overlay.WalkSessionEngine
import com.takwa.lapwalker.ui.main.MainViewModel
import kotlinx.coroutines.CoroutineScope
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val appModule = module {
    // Room Database
    single {
        Room.databaseBuilder(
            androidContext(),
            AppDatabase::class.java,
            "lap_walker_db"
        ).addMigrations(AppDatabase.MIGRATION_1_2)
         .build()
    }
    single { get<AppDatabase>().workoutDao() }
    single { get<AppDatabase>().calisthenicsDao() }

    // DataStore
    single { SettingsDataStore(androidContext()) }

    // Repositories
    single<WorkoutRepository> { WorkoutRepositoryImpl(get()) }
    single<SettingsRepository> { SettingsRepositoryImpl(get()) }
    single<com.takwa.lapwalker.domain.repository.CalisthenicsRepository> { com.takwa.lapwalker.data.repository.CalisthenicsRepositoryImpl(get()) }
    single { com.takwa.lapwalker.data.repository.UpdateRepository(androidContext()) }

    // Use Cases
    factory { RecordLapUseCase() }
    factory { CalculatePaceUseCase() }
    factory { SaveWorkoutUseCase(get()) }
    factory { GetWorkoutsUseCase(get()) }
    factory { DeleteWorkoutUseCase(get()) }
    factory { ClearAllWorkoutsUseCase(get()) }
    factory { GetSettingsUseCase(get()) }
    factory { SaveSettingsUseCase(get()) }
    factory { com.takwa.lapwalker.domain.usecase.GetCalisthenicsProgressUseCase(get()) }
    factory { com.takwa.lapwalker.domain.usecase.RecordExerciseAttemptUseCase(get()) }
    factory { com.takwa.lapwalker.domain.usecase.SeedHistoricWorkoutsUseCase(get()) }

    // Session Engine Factory
    factory { (scope: CoroutineScope) -> WalkSessionEngine(scope) }

    // Step Sensor Manager
    single { com.takwa.lapwalker.core.sensors.StepSensorManager(androidContext()) }

    // ViewModels
    viewModel { MainViewModel(get(), get(), get(), get(), get(), get(), get(), get(), get()) }
    viewModel { com.takwa.lapwalker.ui.calisthenics.ExercisePracticeViewModel(get()) }
}
