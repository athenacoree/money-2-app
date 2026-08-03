package com.example.data.repository

import com.example.data.local.TransactionDao
import com.example.data.local.ProductoDao
import com.example.data.local.EmpleadoDao
import com.example.data.local.ContactoDao
import com.example.data.local.MensajeDao
import com.example.data.local.ConfiguracionDao
import com.example.data.local.AuditoriaStockDao
import com.example.data.local.PropuestaCambioDao
import com.example.data.local.BranchDao
import com.example.data.local.DespachoDistribuidorDao
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
import kotlinx.coroutines.flow.Flow
import java.util.Calendar

class MoneyRepository(
    private val transactionDao: TransactionDao,
    private val productoDao: ProductoDao,
    private val empleadoDao: EmpleadoDao,
    private val contactoDao: ContactoDao,
    private val mensajeDao: MensajeDao,
    private val configuracionDao: ConfiguracionDao,
    private val auditoriaStockDao: AuditoriaStockDao,
    private val propuestaCambioDao: PropuestaCambioDao,
    private val branchDao: BranchDao,
    private val despachoDistribuidorDao: DespachoDistribuidorDao
) {
    // Branches
    val allBranches: Flow<List<BranchInfo>> = branchDao.getAllBranches()

    suspend fun insertBranch(branch: BranchInfo): Long {
        return branchDao.insertBranch(branch)
    }

    suspend fun updateBranch(branch: BranchInfo) {
        branchDao.updateBranch(branch)
    }

    suspend fun deleteBranch(id: Int) {
        branchDao.deleteBranch(id)
    }

    suspend fun deleteAllBranches() {
        branchDao.deleteAllBranches()
    }

    // Distributor Dispatches
    val allDespachos: Flow<List<DespachoDistribuidor>> = despachoDistribuidorDao.getAllDespachos()

    suspend fun insertDespacho(despacho: DespachoDistribuidor): Long {
        return despachoDistribuidorDao.insertDespacho(despacho)
    }

    suspend fun updateDespacho(despacho: DespachoDistribuidor) {
        despachoDistribuidorDao.updateDespacho(despacho)
    }

    suspend fun deleteDespacho(id: Long) {
        despachoDistribuidorDao.deleteDespacho(id)
    }

    suspend fun deleteAllDespachos() {
        despachoDistribuidorDao.deleteAllDespachos()
    }

    // Transactions
    val allTransactions: Flow<List<Transaction>> = transactionDao.getAllTransactions()

    suspend fun insertTransaction(transaction: Transaction): Long {
        return transactionDao.insertTransaction(transaction)
    }

    suspend fun updateTransaction(transaction: Transaction) {
        transactionDao.updateTransaction(transaction)
    }

    suspend fun deleteTransaction(transaction: Transaction) {
        transactionDao.deleteTransaction(transaction.id)
    }

    suspend fun deleteAllTransactions() {
        transactionDao.deleteAllTransactions()
    }

    // Products
    val allProductos: Flow<List<Producto>> = productoDao.getAllProductos()

    suspend fun insertProducto(producto: Producto): Long {
        return productoDao.insertProducto(producto)
    }

    suspend fun updateProducto(producto: Producto) {
        productoDao.updateProducto(producto)
    }

    suspend fun deleteProducto(producto: Producto) {
        productoDao.deleteProducto(producto.id)
    }

    suspend fun deleteAllProductos() {
        productoDao.deleteAllProductos()
    }

    // Employees
    val allEmpleados: Flow<List<Empleado>> = empleadoDao.getAllEmpleados()

    suspend fun insertEmpleado(empleado: Empleado): Long {
        return empleadoDao.insertEmpleado(empleado)
    }

    suspend fun updateEmpleado(empleado: Empleado) {
        empleadoDao.updateEmpleado(empleado)
    }

    suspend fun deleteEmpleado(empleado: Empleado) {
        empleadoDao.deleteEmpleado(empleado.id)
    }

    // Contacts
    val allContactos: Flow<List<Contacto>> = contactoDao.getAllContactos()

    suspend fun insertContacto(contacto: Contacto): Long {
        return contactoDao.insertContacto(contacto)
    }

    suspend fun updateContacto(contacto: Contacto) {
        contactoDao.updateContacto(contacto)
    }

    suspend fun getContactoByTelefono(telefono: String): Contacto? {
        return contactoDao.getContactoByTelefono(telefono)
    }

    suspend fun deleteContacto(contacto: Contacto) {
        contactoDao.deleteContacto(contacto.id)
    }

    suspend fun deleteAllContactos() {
        contactoDao.deleteAllContactos()
    }

    // Messages
    fun getMensajesForContacto(contactoId: Long): Flow<List<Mensaje>> {
        return mensajeDao.getMensajesForContacto(contactoId)
    }

    suspend fun insertMensaje(mensaje: Mensaje): Long {
        return mensajeDao.insertMensaje(mensaje)
    }

    suspend fun deleteMensajesForContacto(contactoId: Long) {
        mensajeDao.deleteMensajesForContacto(contactoId)
    }

    // Configurations
    val allConfiguraciones: Flow<List<Configuracion>> = configuracionDao.getAllConfiguraciones()

    suspend fun getConfiguracionByKey(clave: String): Configuracion? {
        return configuracionDao.getConfiguracionByKey(clave)
    }

    suspend fun insertConfiguracion(configuracion: Configuracion): Long {
        return configuracionDao.insertConfiguracion(configuracion)
    }

    suspend fun deleteAllConfiguraciones() {
        configuracionDao.deleteAllConfiguraciones()
    }

    // Auditorías de Stock
    val allAuditorias: Flow<List<AuditoriaStock>> = auditoriaStockDao.getAllAuditorias()

    suspend fun insertAuditoria(auditoria: AuditoriaStock): Long {
        return auditoriaStockDao.insertAuditoria(auditoria)
    }

    suspend fun deleteAllAuditorias() {
        auditoriaStockDao.deleteAllAuditorias()
    }

    // Propuestas de Cambio (Rama Secundarias de Revisión)
    val allPropuestas: Flow<List<PropuestaCambio>> = propuestaCambioDao.getAllPropuestas()
    val propuestasPendientes: Flow<List<PropuestaCambio>> = propuestaCambioDao.getPropuestasPendientes()

    suspend fun insertPropuesta(propuesta: PropuestaCambio): Long {
        return propuestaCambioDao.insertPropuesta(propuesta)
    }

    suspend fun updatePropuesta(propuesta: PropuestaCambio) {
        propuestaCambioDao.updatePropuesta(propuesta)
    }

    suspend fun deletePropuesta(id: Long) {
        propuestaCambioDao.deletePropuesta(id)
    }

    suspend fun checkAndSeedData() {
        val configExist = configuracionDao.getConfiguracionByKey("nombre_usuario")
        if (configExist == null) {
            configuracionDao.insertConfiguracion(Configuracion(clave = "nombre_usuario", valor = ""))
            configuracionDao.insertConfiguracion(Configuracion(clave = "telefono_usuario", valor = ""))
            configuracionDao.insertConfiguracion(Configuracion(clave = "numero_transfermovil", valor = ""))
            configuracionDao.insertConfiguracion(Configuracion(clave = "tema", valor = "Claro"))
            configuracionDao.insertConfiguracion(Configuracion(clave = "notificaciones", valor = "true"))
            configuracionDao.insertConfiguracion(Configuracion(clave = "lectura_sms", valor = "true"))
            configuracionDao.insertConfiguracion(Configuracion(clave = "modo_empleador_activo", valor = "false"))
            configuracionDao.insertConfiguracion(Configuracion(clave = "modo_empleado_activo", valor = "false"))
        }
    }
}
