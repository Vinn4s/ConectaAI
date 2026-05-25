package com.ConectaAI.demo.service;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.ZoneId;

import org.springframework.stereotype.Service;

@Service
public class TimeContextService {

    private static final ZoneId DEFAULT_ZONE_ID = ZoneId.of("America/Fortaleza");

    public String getCurrentTimeContext() {
        LocalDateTime now = LocalDateTime.now(DEFAULT_ZONE_ID);

        return "Data e hora atual local: " + formatDateTime(now) + "\n" +
               "Dia da semana atual: " + traduzirDiaSemana(now.getDayOfWeek()) + "\n" +
               "Fuso horário: America/Fortaleza.";
    }

    private String formatDateTime(LocalDateTime dateTime) {
        return String.format(
            "%02d/%02d/%04d às %02d:%02d",
            dateTime.getDayOfMonth(),
            dateTime.getMonthValue(),
            dateTime.getYear(),
            dateTime.getHour(),
            dateTime.getMinute()
        );
    }

    private String traduzirDiaSemana(DayOfWeek dayOfWeek) {
        return switch (dayOfWeek) {
            case MONDAY -> "segunda-feira";
            case TUESDAY -> "terça-feira";
            case WEDNESDAY -> "quarta-feira";
            case THURSDAY -> "quinta-feira";
            case FRIDAY -> "sexta-feira";
            case SATURDAY -> "sábado";
            case SUNDAY -> "domingo";
        };
    }
}