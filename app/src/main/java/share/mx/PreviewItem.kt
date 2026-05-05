package share.mx

import android.os.Parcel
import android.os.Parcelable

data class PreviewItem(
    val uri: String,
    val tipo: String,
    val nombre: String = "",
    val duracion: Long = 0L
) : Parcelable {
    constructor(parcel: Parcel) : this(
        uri = parcel.readString() ?: "",
        tipo = parcel.readString() ?: "",
        nombre = parcel.readString() ?: "",
        duracion = parcel.readLong()
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(uri)
        parcel.writeString(tipo)
        parcel.writeString(nombre)
        parcel.writeLong(duracion)
    }

    override fun describeContents(): Int = 0

    companion object CREATOR : Parcelable.Creator<PreviewItem> {
        override fun createFromParcel(parcel: Parcel): PreviewItem = PreviewItem(parcel)
        override fun newArray(size: Int): Array<PreviewItem?> = arrayOfNulls(size)
    }
}