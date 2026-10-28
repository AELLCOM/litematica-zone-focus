# Litematica Zone Focus

Copyright (c) 2026 Aell_

Лицензия: GNU GPL v3 или новее (см. LICENSE) со следующим дополнительным
условием согласно разделу 7(c) GPLv3:

> Изменённые версии этой программы должны распространяться под названием,
> отличным от «Litematica Zone Focus», и не должны выдавать себя за
> оригинальную программу. Имя автора (Aell_) не может использоваться для
> продвижения производных работ без письменного разрешения.

Additional term under GPLv3 section 7(c):

> Modified versions of this program must be distributed under a name different
> from "Litematica Zone Focus" and must not present themselves as the original
> program. The author's name (Aell_) may not be used to promote derivative
> works without written permission.

## Зависимости

- **Litematica** и **malilib** (masa / sakura-ryoko) — LGPLv3. Мод обращается к
  ним как к внешним библиотекам, их код не включён в сборку.
- **Litematica Material HUD** (Aell_) — GPL-3.0-or-later, опциональная интеграция.

## Встроенный код

- **aell-ui** (Aell_) — MIT. Общий модуль экрана настроек, исходники лежат в
  `aell-ui/`. При сборке он переносится в пакет
  `com.aell.zonefocus.shaded.ui`, чтобы два мода с этим модулем не делили
  одинаковые классы в одинаковом пакете. MIT-код разрешено включать в
  GPL-проект; обратное направление заимствования недопустимо, поэтому в
  общем модуле нет ни строчки из Zone Focus.
