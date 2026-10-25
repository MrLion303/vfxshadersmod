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

Los VFX se renderizan en cada cliente; el servidor sincroniza las órdenes, no buffers gráficos. El código GLSL aplica una distorsión cinematográfica de pantalla y no modifica la geometría ni la física de los bloques.

## Efectos incluidos

- `demo_orbit`: anillos de demostración alrededor del jugador local.
- `music_horizon_bars`: barras de neón alrededor del horizonte.
- `rainbow_aurora`: cortinas de luz arcoíris.
- `rainbow_sky`: cielo de color uniforme, alternando tonos del arcoíris, con cenit cerrado y nubes teñidas.
- `blackhole`: agujero negro en coordenadas del mundo, disco de acreción y distorsión fuerte del fondo.
- `blackhole_sky`: agujero negro celeste gigante, fijo a 50° sobre el horizonte, con disco y lente gravitacional.

