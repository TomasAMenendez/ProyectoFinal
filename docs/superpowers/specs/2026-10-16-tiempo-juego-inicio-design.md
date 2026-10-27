# Tiempo de juego restante — diseño

Fecha: 2026-10-16  
Producto: app Android TEO  
Estado: aprobado en conversación; pendiente de plan de implementación

## Problema

El peluche ya cuenta el tiempo despierto, publica lo que queda y, al prenderlo desde la app, vuelve a llenar el contador. Si el tiempo se acaba, el botón físico no lo despierta. La app no muestra ese restante en Inicio ni permite fijar el total desde Mi TEO.

## Decisiones

- La app muestra el cálculo del peluche. No lleva un reloj propio.
- El tiempo baja mientras TEO está despierto, se pausa si se duerme antes de acabarse y, al prenderlo desde la app, vuelve al total. Ese comportamiento ya está en el peluche y no se modifica.
- El total se escribe a mano, de 0 a 240 minutos. 0 significa sin límite.
- «Aplicar tiempo» envía solo el total. Volumen, brillo y pulsador quedan fuera de ese envío.

## Inicio

Dentro de la tarjeta «Hoy con TEO», debajo de las interacciones y la última señal, a lo ancho de la tarjeta:

- Un valor en `mm:ss`, por ejemplo `45:00`.
- Debajo, una línea horizontal. El tramo recorrido usa el azul petróleo y el resto el trazo gris. Llena cuando queda todo el tiempo y vacía en `00:00`.
- Sin controles. Se actualiza con el estado que Inicio ya consulta cada 5 segundos.

La función pura que lee el estado devuelve uno de tres resultados:

| Resultado | Condición | Pantalla |
|---|---|---|
| Oculto | `playtime_limit_minutes` es 0 | El bloque no aparece |
| Ausente | Falta el límite, el restante o la fracción, o TEO está desconectado | El valor usa el texto de ausencia de la tarjeta y la línea no se dibuja |
| Visible | Límite mayor que 0 y llegan `playtime_remaining_seconds` y `playtime_remaining_fraction` | Tiempo y línea |

Un cero conocido, con límite mayor que 0 y restante 0, es visible: `00:00` y la línea vacía. Los segundos van siempre con dos dígitos y los minutos con al menos dos (`05:00`, `45:00`, `240:00`). Una fracción fuera de 0 a 1 se recorta a ese rango. El texto sale de los segundos restantes. La línea usa la fracción que manda TEO.

La vista previa muestra el mismo bloque con un ejemplo fijo: 30 minutos restantes de 60, texto `30:00` y fracción `0,5`.

## Mi TEO

En la tarjeta de volumen y brillo, debajo del botón que aplica el sonido y el brillo, y antes de la tarjeta de modo noche:

- El total confirmado, en minutos o como «Sin límite».
- Un campo numérico de hasta 3 dígitos, con sufijo `min`.
- El botón «Aplicar tiempo».

El botón se habilita cuando el número escrito es un entero de 0 a 240 y es distinto del total confirmado. Vacío, no numérico o fuera de rango lo deja deshabilitado y no genera envío.

El cuerpo es `{ "playtime_limit_minutes": N }` hacia `POST /api/config`. El peluche ya aplica solo las claves presentes. El valor pasa a confirmado cuando la respuesta trae `playtime_limit_minutes` igual al número enviado. Hasta entonces el campo sigue con lo escrito y no se anuncia éxito.

Si la red falla, o la respuesta no trae ese mismo número, el aviso dice que no se pudo guardar.

## Alcance

- Pantalla real de Inicio y su vista previa, con el mismo bloque. La vista previa usa un ejemplo y no consulta al peluche.
- Ajustes de Mi TEO y el cliente que arma el cuerpo del tiempo.
- Una frase de la constitución: el tiempo de uso deja de figurar como decisión aplazada. Pulsador y acceso a la agenda sin TEO siguen aplazados. El comportamiento permitido es el de este documento.

Quedan fuera el reloj, el bloqueo del botón físico y el reinicio al prender. También quedan fuera un temporizador local, contar solo la interacción efectiva y cambiar el rango 0 a 240.

## Pruebas

- Límite 0 queda oculto.
- Dato faltante queda ausente.
- 90 segundos se leen `01:30`. 300 segundos se leen `05:00`. 14400 segundos se leen `240:00`.
- Restante 0 con límite mayor que 0 se lee `00:00` y la fracción útil es 0.
- El cuerpo del tiempo solo contiene `playtime_limit_minutes`.
- El cuerpo de volumen y brillo sigue sin ese campo.
- Un número inválido no arma cuerpo.
- La confirmación exige el eco del mismo número.
