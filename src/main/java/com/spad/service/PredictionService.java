package com.spad.service;

import com.spad.dto.Dtos.Coefficient;
import com.spad.dto.Dtos.PredictionDto;
import com.spad.dto.Dtos.SemesterStat;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Predicts a student's next-semester SGPA with multiple linear regression, trained on every
 * semester in the database:  SGPA = b0 + b1*studyHours + b2*attendance + b3*assignmentAvg + b4*previousCgpa.
 * The coefficients are solved with the normal equations (no external ML library needed).
 * With too little data it falls back to a fixed heuristic.
 */
@Service
public class PredictionService {

    private static final int MIN_ROWS = 12;
    private static final double[] HEURISTIC = {0.0, 0.06, 0.02, 0.017, 0.55};

    public record Model(double[] coef, double r2, int rows, boolean regression) {}

    public Model train(List<AnalyticsService.Prepared> all) {
        List<double[]> xs = new ArrayList<>();
        List<Double> ys = new ArrayList<>();
        for (AnalyticsService.Prepared p : all) {
            List<SemesterStat> st = p.stats();
            for (int i = 1; i < st.size(); i++) {
                SemesterStat cur = st.get(i);
                SemesterStat prev = st.get(i - 1);
                if (cur.studyHours() <= 0) continue;
                xs.add(new double[]{1.0, cur.studyHours(), cur.attendance(), cur.assignmentAvg(), prev.cgpa()});
                ys.add(cur.sgpa());
            }
        }
        if (xs.size() >= MIN_ROWS) {
            double[] coef = fit(xs, ys);
            if (coef != null) {
                double mean = ys.stream().mapToDouble(Double::doubleValue).average().orElse(0);
                double ssTot = 0;
                double ssRes = 0;
                for (int k = 0; k < xs.size(); k++) {
                    double predicted = 0;
                    for (int j = 0; j < coef.length; j++) predicted += coef[j] * xs.get(k)[j];
                    ssRes += Math.pow(ys.get(k) - predicted, 2);
                    ssTot += Math.pow(ys.get(k) - mean, 2);
                }
                double r2 = ssTot > 0 ? Math.max(0, 1 - ssRes / ssTot) : 0;
                return new Model(coef, r2, xs.size(), true);
            }
        }
        return new Model(HEURISTIC, 0, xs.size(), false);
    }

    /** Ordinary least squares via Gauss-Jordan elimination on the normal equations. */
    private static double[] fit(List<double[]> xs, List<Double> ys) {
        int p = xs.get(0).length;
        double[][] a = new double[p][p + 1];
        for (int k = 0; k < xs.size(); k++) {
            double[] x = xs.get(k);
            double y = ys.get(k);
            for (int i = 0; i < p; i++) {
                for (int j = 0; j < p; j++) a[i][j] += x[i] * x[j];
                a[i][p] += x[i] * y;
            }
        }
        for (int i = 1; i < p; i++) a[i][i] += 1e-6; // tiny ridge term keeps the matrix invertible

        for (int col = 0; col < p; col++) {
            int pivot = col;
            for (int r = col + 1; r < p; r++) {
                if (Math.abs(a[r][col]) > Math.abs(a[pivot][col])) pivot = r;
            }
            if (Math.abs(a[pivot][col]) < 1e-12) return null;
            double[] tmp = a[col];
            a[col] = a[pivot];
            a[pivot] = tmp;
            for (int r = 0; r < p; r++) {
                if (r == col) continue;
                double f = a[r][col] / a[col][col];
                for (int c = col; c <= p; c++) a[r][c] -= f * a[col][c];
            }
        }
        double[] coef = new double[p];
        for (int i = 0; i < p; i++) {
            coef[i] = a[i][p] / a[i][i];
            if (!Double.isFinite(coef[i])) return null;
        }
        return coef;
    }

    public double predict(Model m, double study, double attendance, double assignment, double previousCgpa) {
        double[] c = m.coef();
        double v = c[0] + c[1] * study + c[2] * attendance + c[3] * assignment + c[4] * previousCgpa;
        return Grading.round2(Math.max(0, Math.min(10, v)));
    }

    public static String risk(double predictedSgpa, double attendance) {
        if (predictedSgpa < 5.5 || attendance < 65) return "HIGH";
        if (predictedSgpa < 7.0 || attendance < 75) return "MEDIUM";
        return "LOW";
    }

    public PredictionDto describe(Model m, double study, double attendance, double assignment,
                                  double previousCgpa, int targetSemester) {
        double predicted = predict(m, study, attendance, assignment, previousCgpa);
        double[] c = m.coef();
        List<Coefficient> coefficients = List.of(
                new Coefficient("Study hours per week", Grading.round2(c[1] * 1000) / 1000),
                new Coefficient("Attendance (%)", Grading.round2(c[2] * 1000) / 1000),
                new Coefficient("Assignment score", Grading.round2(c[3] * 1000) / 1000),
                new Coefficient("Previous CGPA", Grading.round2(c[4] * 1000) / 1000));
        return new PredictionDto(targetSemester, predicted, risk(predicted, attendance),
                m.regression() ? "Linear regression" : "Heuristic (not enough data to train)",
                Grading.round2(m.r2()), m.rows(), Grading.round2(c[0] * 1000) / 1000,
                coefficients, advice(study, attendance, assignment, previousCgpa));
    }

    public PredictionDto forStudent(Model m, AnalyticsService.Prepared p) {
        SemesterStat last = p.latest();
        double study = last.studyHours() > 0 ? last.studyHours() : averageStudy(p);
        return describe(m, study, last.attendance(), last.assignmentAvg(), last.cgpa(), last.semester() + 1);
    }

    private static double averageStudy(AnalyticsService.Prepared p) {
        return p.stats().stream().mapToDouble(SemesterStat::studyHours).filter(h -> h > 0).average().orElse(10);
    }

    public static List<String> advice(double study, double attendance, double assignment, double previousCgpa) {
        List<String> tips = new ArrayList<>();
        if (attendance < 75) {
            tips.add("Attendance is below 75%. Attending more classes is the fastest way to protect the grade.");
        }
        if (study < 8) {
            tips.add("Weekly study time is low. Aim for at least 10 hours a week.");
        }
        if (assignment < 60) {
            tips.add("Assignment scores are weak. Submit every assignment and revise the feedback.");
        }
        if (previousCgpa < 6) {
            tips.add("The CGPA base is low, so steady improvement in every subject matters more than one big jump.");
        }
        if (tips.isEmpty()) {
            tips.add("Current habits look healthy. Keep attendance, study hours and assignments steady.");
        }
        return tips;
    }
}
