import { useState } from 'react'

/** PLACEHOLDER · Tareas 3 y 4 son de Jordi. Aquí se escribe un id a mano para probar el detalle. */
export default function ListaUsuarios({ onSeleccionar }) {
  const [id, setId] = useState('')
  return (
    <section className="lista">
      <h1>Usuarios registrados</h1>
      <p className="suave">Pendiente (Jordi). Para probar tu panel, escribe el id de un usuario:</p>
      <div className="filtros">
        <input value={id} onChange={(e) => setId(e.target.value)} placeholder="id del usuario" aria-label="Id de usuario" />
        <button type="button" className="boton" onClick={() => id && onSeleccionar(id)}>
          Ver detalle
        </button>
      </div>
    </section>
  )
}
