package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.model.Transaction
import com.example.data.model.Producto
import com.example.data.model.Empleado
import com.example.data.model.Contacto
import com.example.data.model.Mensaje
import com.example.data.model.Configuracion
import com.example.data.model.AuditoriaStock
import com.example.data.model.PropuestaCambio
import com.example.data.model.BranchInfo
import com.example.data.model.DespachoDistribuidor
import com.example.data.model.SaldoMovil

@Database(
    entities = [
        Transaction::class,
        Producto::class,
        Empleado::class,
        Contacto::class,
        Mensaje::class,
        Configuracion::class,
        AuditoriaStock::class,
        PropuestaCambio::class,
        BranchInfo::class,
        DespachoDistribuidor::class,
        SaldoMovil::class
    ],
    version = 9,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao
    abstract fun productoDao(): ProductoDao
    abstract fun empleadoDao(): EmpleadoDao
    abstract fun contactoDao(): ContactoDao
    abstract fun mensajeDao(): MensajeDao
    abstract fun configuracionDao(): ConfiguracionDao
    abstract fun auditoriaStockDao(): AuditoriaStockDao
    abstract fun propuestaCambioDao(): PropuestaCambioDao
    abstract fun branchDao(): BranchDao
    abstract fun despachoDistribuidorDao(): DespachoDistribuidorDao
    abstract fun saldoMovilDao(): SaldoMovilDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "money_v6_clean.db"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
