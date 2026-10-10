package com.spad.service;

import com.spad.model.Mark;

/** Grading rules on a 10-point scale. Total = 20% assignment + 30% internal + 50% exam. */
public final class Grading {

    private Grading() {}

    public static double total(Mark m) {
        return 0.2 * m.getAssignmentScore() + 0.3 * m.getInternalScore() + 0.5 * m.getExamScore();
    }

    public static int gradePoint(double total) {
        if (total >= 90) return 10;
        if (total >= 80) return 9;
        if (total >= 70) return 8;
        if (total >= 60) return 7;
        if (total >= 50) return 6;
        if (total >= 40) return 5;
        return 0;
    }

    public static String letter(int gradePoint) {
        return switch (gradePoint) {
            case 10 -> "O";
            case 9 -> "A+";
            case 8 -> "A";
            case 7 -> "B+";
            case 6 -> "B";
            case 5 -> "C";
            default -> "F";
        };
    }

    public static double round1(double v) {
        return Math.round(v * 10.0) / 10.0;
    }

    public static double round2(double v) {
        return Math.round(v * 100.0) / 100.0;
    }
}
