package bg.energo.phoenix.service.xEnergie.jobs.event;

import bg.energo.phoenix.service.xEnergie.jobs.enums.XEnergieJobType;

import java.time.LocalDate;

public record XEnergieJobCompletionEvent(LocalDate date, XEnergieJobType job) {
}
