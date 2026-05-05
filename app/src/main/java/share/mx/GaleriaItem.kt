package share.mx

sealed class GaleriaItem {
    data class HeaderMes(val titulo: String) : GaleriaItem()
    data class HeaderDia(val titulo: String) : GaleriaItem()
    data class ArchivoContenido(val archivo: ArchivoItem) : GaleriaItem()
}