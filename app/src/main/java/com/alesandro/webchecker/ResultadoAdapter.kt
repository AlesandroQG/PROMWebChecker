package com.alesandro.webchecker

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

/**
 * Adapter del RecyclerView de resultados
 */
class ResultadoAdapter(private val resultadoList: MutableList<Resultado>, private val onDeleteClicked: (Resultado) -> Unit) : RecyclerView.Adapter<ResultadoAdapter.ViewHolder>() {
    /**
     * Clase ViewHolder
     */
    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val url: TextView = view.findViewById(R.id.url)
        private val resultadoTV: TextView = view.findViewById(R.id.resultado)
        private val eliminar: ImageButton = view.findViewById(R.id.eliminar)

        /**
         * Función que carga el resultado a la vista item
         *
         * @param resultado
         */
        fun render(resultado: Resultado){
            url.text = resultado.url
            resultadoTV.text = resultado.resultado
            eliminar.setOnClickListener {
                onDeleteClicked(resultado)
            }
        }
    }

    /**
     * Función al crear el ViewHolder
     *
     * @param parent
     * @param viewType
     * @return ViewHolder
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_resultado, parent, false)
        return ViewHolder(view)
    }

    /**
     * Función para cargar el resultado a la vista item
     *
     * @param holder
     * @param position posición en la lista
     */
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.render(resultadoList[position])
    }

    /**
     * Función para devolver el tamaño de la lista
     *
     * @return tamaño de la lista
     */
    override fun getItemCount(): Int {
        return resultadoList.size
    }

    /**
     * Función para eliminar un resultado de la lista
     *
     * @param resultado a eliminar
     */
    fun removeResultado(resultado: Resultado) {
        val position = resultadoList.indexOf(resultado)
        if (position != -1) {
            resultadoList.removeAt(position)
            notifyItemRemoved(position)
        }
    }
}