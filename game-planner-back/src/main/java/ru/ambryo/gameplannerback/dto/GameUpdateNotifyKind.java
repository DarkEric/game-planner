package ru.ambryo.gameplannerback.dto;

/**
 * Какое уведомление отправить после правки игры (не более одного за запрос).
 */
public enum GameUpdateNotifyKind {
    RESCHEDULE,
    TITLE_CHANGE
}
