# Contract: Registro en la planilla

Aplica solo a la versión publicada. La versión autónoma no envía esta solicitud y no contiene la URL ni la clave.

## Configuración

| Nombre | Uso |
|---|---|
| URL del Web App | Destino del POST. Vacía si todavía no se desplegó la planilla. |

Si falta la URL, la partida termina igual y no se intenta el envío.

## Solicitud

`POST` con cuerpo `text/plain` y JSON adentro:

```json
{
  "Nombre": "Nombre visible",
  "Juego": "Sombras",
  "Resultado": "Ganó"
}
```

| Campo | Valores |
|---|---|
| Nombre | Texto ya validado de la visita, hasta 40 caracteres |
| Juego | `Sombras`, `Adivina el objeto`, `Contar` |
| Resultado | `Ganó`, `Perdió` |

La fecha y hora las escribe el programa de la hoja al agregar la fila. Se envía una vez por partida, cuando el resultado pasa a ganado o perdido. No se envía al abandonar una partida sin cierre. La interfaz no espera ni muestra el resultado del envío.

## Programa de la planilla

`AppParaExpo/planilla-apps-script.txt` contiene el script para pegar en el editor de la hoja. Al recibirse un POST con cuerpo JSON:

1. Lee `Nombre`, `Juego` y `Resultado`.
2. Agrega una fila en ese orden y pone la fecha y hora del servidor.
3. Si no hay cuerpo, no agrega una fila.

La hoja se llama `Hoja 1`. El despliegue del Web App queda documentado en el README: ejecución como quien mantiene la hoja y acceso para cualquier persona.
