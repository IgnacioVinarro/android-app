package com.ignaciovinarro.stayfocused.gym.domain

// En la base estos tipos se guardan como texto (nombre del enum), igual que en Finanzas.

enum class MuscleGroup { CHEST, BACK, SHOULDERS, BICEPS, TRICEPS, FOREARMS, ABS, QUADS, HAMSTRINGS, GLUTES, CALVES, FULL_BODY, CARDIO }

enum class Equipment { BARBELL, DUMBBELL, MACHINE, CABLE, BODYWEIGHT, KETTLEBELL, BAND, OTHER }

// Qué se anota en cada serie (GY-01).
enum class TrackingType { WEIGHT_REPS, TIME, REPS_ONLY }

// Tipos de serie (GY-12).
enum class SetType { WARMUP, NORMAL, FAILURE, DROP }

enum class SessionState { IN_PROGRESS, COMPLETED, ABANDONED }

// Récords personales (GY-09).
enum class RecordKind { MAX_WEIGHT, MAX_REPS_AT_WEIGHT, MAX_VOLUME, ESTIMATED_1RM }
