# Comandos de VFX

Los comandos se registran localmente en el cliente. Los VFX visuales son locales a esa instancia y todavía no se sincronizan con otros jugadores.

## Comandos disponibles

| Comando | Función |
|---|---|
| /vfx o /vfx help | Muestra la ayuda |
| /vfx list | Enumera los VFX, categorías y estados |
| /vfx enable <efecto> | Activa un efecto global |
| /vfx disable <efecto> | Desactiva un efecto |
| /vfx toggle <efecto> | Alterna su estado |
| /vfx status <efecto> | Consulta el estado de un efecto |
| /vfx disable all | Limpia todos los VFX locales |
| /vfx enable blackhole <tamaño> <x> <y> <z> | Coloca un agujero negro en coordenadas del mundo |
| /vfx disable blackhole | Elimina todos los agujeros negros colocados |
| /vfx enable blackhole_sky | Activa el agujero negro celeste |
| /vfx disable blackhole_sky | Desactiva el agujero negro celeste |

El tamaño del agujero negro del mundo representa el diámetro de su horizonte en bloques, de 1 a 100. El valor de referencia es 10 bloques. Las coordenadas admiten valores absolutos y relativos con ~.

Ejemplos:

\`\`\`text
/vfx enable blackhole 10 100 64 -200
/vfx enable blackhole 24 ~ ~5 ~-12
/vfx disable blackhole
/vfx enable blackhole_sky
/vfx disable blackhole_sky
/vfx enable rainbow_sky
/vfx enable rainbow_aurora
/vfx enable music_horizon_bars
/vfx disable all
\`\`\`

## VFX incluidos

- demo_orbit: tres anillos de color alrededor del jugador local. Es un efecto de prueba del motor.
- music_horizon_bars: barras de ecualizador animadas alrededor del horizonte. Sus ondas usan frecuencias periódicas que cierran exactamente el círculo; el movimiento aún no analiza audio real.
- rainbow_aurora: cortinas semitransparentes ondulantes alrededor del cielo, con cierre continuo y colores arcoíris.
- rainbow_sky: una cúpula translúcida que tiñe el cielo y recorre gradualmente el arcoíris.
- blackhole: una aparición fija en las coordenadas indicadas, con horizonte negro, disco de acreción y lente gravitacional. Puedes acercarte o atravesar su centro; al entrar en el horizonte deja de dibujarse la esfera opaca y aumenta la deformación de la escena.
- blackhole_sky: un agujero negro celeste con disco de acreción y lente gravitacional que afecta la imagen del cielo y del mundo alrededor de su posición proyectada.

La lente gravitacional modifica la imagen que ve el cliente. No altera las coordenadas reales de los bloques ni aplica fuerzas físicas, y todavía no se replica a otros jugadores.

## Cómo añadir otro efecto

1. Crear una implementación de VfxEffect.
2. Elegir una categoría en VfxEffectCategory.
3. Registrar la definición en ClientVfxCatalog.
4. Si es un objeto colocado, enlazarlo a un ClientWorldVfxRenderer.
5. Si deforma la imagen completa, conectar el pase GLSL al pipeline de pantalla.
6. Probar el efecto dentro de Minecraft; una compilación correcta no garantiza su apariencia en todos los drivers.
