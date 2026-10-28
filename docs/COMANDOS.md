# Comandos del mod

## Comandos VFX

Los comandos `/vfx` se pueden usar en el chat y desde bloques de comandos. Los bloques ejecutan la versión del servidor, que envía cada orden a todos los clientes que estén en la misma dimensión. Para eso, el mod debe estar instalado en el servidor y en cada cliente. Requieren permiso de operador de nivel 2.

| Comando | Función |
|---|---|
| `/vfx list` | Enumera los VFX incluidos |
| `/vfx enable <efecto>` | Activa un efecto |
| `/vfx disable <efecto>` | Desactiva un efecto |
| `/vfx toggle <efecto>` | Alterna su estado |
| `/vfx status <efecto>` | Muestra el estado de la última orden del servidor |
| `/vfx disable all` | Limpia los efectos activos y las instancias colocadas |
| `/vfx enable blackhole <tamaño> <x> <y> <z>` | Coloca un agujero negro en coordenadas del mundo |
| `/vfx disable blackhole` | Elimina todos los agujeros negros colocados |
| `/vfx enable blackhole_sky` | Activa el agujero negro celeste gigante |
| `/vfx disable blackhole_sky` | Desactiva el agujero celeste |
| `/vfx enable rainbow_sky` | Colorea uniformemente el cielo y las nubes |
| `/vfx disable rainbow_sky` | Desactiva el cielo de colores |
| `/vfx enable rainbow_aurora_photon` | Activa la variante Photon de la aurora arcoíris |
| `/vfx disable rainbow_aurora_photon` | Desactiva esa variante |

El tamaño del agujero negro del mundo es el diámetro de su horizonte en bloques (de 1 a 100). Las coordenadas aceptan números absolutos o relativos con `~`.

## Agujero negro celeste

`blackhole_sky` no utiliza la mirada de quien ejecuta el comando. Se coloca en una dirección fija del mundo, a 50° sobre el horizonte (0° = horizonte; 90° = cenit), con un diámetro aparente gigantesco. Usa disco blanco-dorado y halos de luz inspirados en la apariencia cinematográfica de Gargantúa en *Interstellar*.

## Impulso de jugadores

`/vfxmove empujon <jugadores>` se ejecuta en el servidor. El impulso horizontal se mantiene y la componente vertical se incrementó para crear un salto parabólico más alto.

Ejemplos:

```text
/vfxmove empujon @a
/vfxmove empujon @p
/vfxmove empujon LAYONv
/vfxmove empujon @a[distance=..10]
```

## Ejemplos para bloques de comandos

```text
/vfx enable blackhole 80 120 170 -300
/vfx disable blackhole
/vfx enable blackhole_sky
/vfx disable blackhole_sky
/vfx enable rainbow_sky
/vfx disable rainbow_sky
/vfxmove empujon @a
```

Los VFX se renderizan en cada cliente; el servidor sincroniza las órdenes, no buffers gráficos. La lente gravitacional de pantalla está activa alrededor del agujero negro colocado. Si la cámara queda dentro o demasiado cerca del horizonte, el objeto se oculta temporalmente para evitar que la esfera negra envuelva toda la vista; este efecto no altera los bloques ni sus colisiones.

## Efectos incluidos

- `demo_orbit`: anillos de demostración alrededor del jugador local.
- `music_horizon_bars`: barras de neón alrededor del horizonte.
- `rainbow_aurora`: cortinas de luz arcoíris del renderizador clásico.
- `rainbow_aurora_photon`: variante separada de partículas y estelas Photon.
- `rainbow_sky`: cielo de color uniforme, alternando tonos del arcoíris, con cenit cerrado y nubes teñidas.
- `blackhole`: agujero negro en coordenadas del mundo, disco de acreción y distorsión fuerte del fondo.
- `blackhole_sky`: agujero negro celeste gigante, fijo a 50° sobre el horizonte, con disco y lente gravitacional.



## Movimiento por jugador

- `/vfxmove moonjump enable <jugadores>`: reduce la gravedad efectiva para conseguir saltos altos y caídas lentas.
- `/vfxmove moonjump disable <jugadores>`: restaura el movimiento normal.
- `/vfxmove waterwalk enable <jugadores>`: permite desplazarse sobre la superficie del agua sin hundirse.
- `/vfxmove waterwalk disable <jugadores>`: desactiva el efecto. `caminar_agua` es un alias.
- `<jugadores>` acepta nombres o selectores como `@a`, `@p` y `@s`.

## Efectos nuevos

- `/vfx enable cloud_vortex` y `/vfx disable cloud_vortex`: vórtice de nubes celeste naranja rojizas en volumen 3D, con turbulencia y centro luminoso guiados por la imagen de referencia.
- `/vfx enable starsspeed <color>`: cielo de color uniforme con estelas rápidas y luz en el horizonte. El color es opcional; por defecto, azul.
- `/vfx enable ground_aurora <x> <y> <z> <color>`: ondas de aurora sobre el terreno que se expanden desde el punto indicado.
- `/vfx enable falling_stars` y `/vfx disable falling_stars`: lluvia de estrellas arcoíris diagonales.
- `/vfx enable sky_portal` y `/vfx disable sky_portal`: portal cinematográfico en el cielo.

## Efectos de un solo uso

`/vfxonce scan <x> <y> <z> <color>` inicia un aro de escaneo desde las coordenadas indicadas. El aro se adapta al relieve del terreno cargado y termina solo; no hace falta ejecutar otro comando para detenerlo.

`/vfxonce trampolin <x> <y> <z> [potencia] [radio] [velocidad]` crea un pulso circular de un solo uso. El frente se expande por el suelo, la malla visual se eleva en crestas concéntricas y los jugadores que estén en el suelo reciben un impulso vertical cuando la onda los alcanza. La potencia representa la altura aproximada del salto en bloques; el radio se mide en bloques y la velocidad en bloques por tick. Los valores por defecto son potencia `12`, radio `32` y velocidad `0.65`. Límites: potencia `1–64`, radio `4–64` y velocidad `0.15–2`.

Ejemplos:

```text
/vfxonce trampolin 0 64 0
/vfxonce trampolin 120 70 -300 24
/vfxonce trampolin ~ ~ ~ 32 48 0.8
```

La superficie ondulada es una deformación visual temporal sobre el terreno cargado; no sustituye las mallas ni las colisiones de los bloques. El impulso de los jugadores sí se aplica desde el servidor, y la gravedad normal crea la trayectoria parabólica.

Colores básicos aceptados: `azul`, `celeste`, `cian`, `rojo`, `naranja`, `amarillo`, `verde`, `morado`, `rosa` y `blanco`. También se acepta hexadecimal con el formato `#RRGGBB`.



## Cámara y efectos Photon de un solo uso

```text
/vfxcamara hombro @s
/vfxcamara hombro @a
/vfxcamara fija 100 75 -20 1.5 "abajo, sur" @a
/vfxcamara fija 100 75 -20 false norte @p
/vfxcamara fijadesactivar 1.2 @a
/vfxcamara fijadesactivar false @s
/vfxonce confeti ~ ~1 ~
/vfxonce llamas 100 64 -20
```

- `/vfxcamara hombro <objetivos>` alterna entre primera persona y una vista de tercera persona situada detrás y a un lado del jugador, con transición suave. Una segunda ejecución devuelve la vista a primera persona. Acepta selectores como `@s`, `@p`, `@a` y `@e[type=player]`.
- `/vfxcamara fija <x> <y> <z> <tiempo|false> <dirección> [objetivos]` coloca la cámara en esas coordenadas absolutas del mundo (o relativas si usas `~`) y la orienta hacia la dirección indicada. La cámara permanece en ese punto mientras el jugador se mueve y el modelo del jugador sigue visible desde la cámara. El tiempo se expresa en segundos, entre 0 y 30; `false` o `0` hacen el cambio instantáneo. Si se omiten los objetivos, el jugador que ejecuta el comando es el objetivo.
- `/vfxcamara fijadesactivar <tiempo|false> [objetivos]` devuelve la cámara al jugador, con transición o de inmediato. Los objetivos se pueden omitir desde el chat.
- Direcciones válidas: `arriba`, `abajo`, los ocho rumbos de brújula y combinaciones con arriba/abajo. Por ejemplo, `"abajo, sur"` mira en diagonal hacia abajo y al sur. Las direcciones con espacios deben escribirse entre comillas.
- `/vfxonce confeti <x> <y> <z>` usa un cono de emisión Photon que dispara el confeti hacia arriba; la gravedad hace que pierda altura y caiga de forma natural. La ráfaga y la caída permanecen visibles varios segundos.
- `/vfxonce llamas <x> <y> <z>` dispara una columna de chispas cálidas en un cono estrecho, con estelas y bloom, como un cañón pirotécnico de concierto. Es un efecto de partículas, no fuego colocado en el mundo.

Los comandos requieren permiso de operador de nivel 2. La cámara usa al propio jugador como entidad de cámara; solo altera la vista del cliente, no bloquea el movimiento ni modifica las coordenadas del jugador. La vista de hombro conserva el giro del ratón y añade comprobación de colisiones para evitar meter la cámara en bloques. Las ráfagas se ejecutan en las coordenadas indicadas y los emisores Photon se destruyen al terminar.
