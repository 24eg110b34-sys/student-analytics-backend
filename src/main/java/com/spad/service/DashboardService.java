package com.spad.service;

import com.spad.dto.Dtos.Bucket;
import com.spad.dto.Dtos.DeptAverage;
import com.spad.dto.Dtos.FacultyOverview;
import com.spad.dto.Dtos.Highlight;
import com.spad.dto.Dtos.PredictRequest;
import com.spad.dto.Dtos.PredictionDto;
import com.spad.dto.Dtos.SemesterStat;
import com.spad.dto.Dtos.StudentDashboard;
import com.spad.dto.Dtos.StudentSummary;
import com.spad.dto.Dtos.SubjectResult;
import com.spad.dto.Dtos.TrendPoint;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

@Service
public class DashboardService {

    private final AnalyticsService analytics;
    private final PredictionService predictions;

    public DashboardService(AnalyticsService analytics, PredictionService predictions) {
        this.analytics = analytics;
        this.predictions = predictions;
    }

    @Transactional(readOnly = true)
    public StudentDashboard studentDashboard(Long studentId) {
        AnalyticsService.Prepared me = analytics.prepare(studentId);
        List<AnalyticsService.Prepared> all = analytics.prepareAll();
        PredictionService.Model model = predictions.train(all);

        List<AnalyticsService.Prepared> withData = all.stream().filter(AnalyticsService.Prepared::hasData).toList();
        double classAvg = withData.stream().mapToDouble(AnalyticsService.Prepared::cgpa).average().orElse(0);

        if (!me.hasData()) {
            return new StudentDashboard(AnalyticsService.info(me.data().student()), 0, 0, 0,
                    Grading.round2(classAvg), 0, withData.size(), 0,
                    List.of(), List.of(), List.of(), List.of(), null);
        }

        SemesterStat latest = me.latest();
        int rank = 1;
        for (AnalyticsService.Prepared o : withData) {
            if (o.cgpa() > me.cgpa()) rank++;
        }

        List<SubjectResult> subjects = analytics.subjectResults(me.data(), latest.semester());
        List<SubjectResult> byTotal = new ArrayList<>(subjects);
        byTotal.sort(Comparator.comparingDouble(SubjectResult::total).reversed());
        int n = Math.min(2, byTotal.size());
        List<Highlight> strongest = byTotal.subList(0, n).stream()
                .map(s -> new Highlight(s.name(), s.total())).toList();
        List<Highlight> weakest = byTotal.subList(byTotal.size() - n, byTotal.size()).stream()
                .map(s -> new Highlight(s.name(), s.total()))
                .sorted(Comparator.comparingDouble(Highlight::total))
                .toList();

        return new StudentDashboard(AnalyticsService.info(me.data().student()),
                me.cgpa(), latest.sgpa(), latest.attendance(),
                Grading.round2(classAvg), rank, withData.size(), latest.semester(),
                me.stats(), subjects, strongest, weakest, predictions.forStudent(model, me));
    }

    @Transactional(readOnly = true)
    public List<StudentSummary> students(String query) {
        List<AnalyticsService.Prepared> all = analytics.prepareAll();
        PredictionService.Model model = predictions.train(all);
        String q = query == null ? "" : query.trim().toLowerCase();
        return all.stream()
                .map(p -> summary(p, model))
                .filter(s -> q.isEmpty()
                        || s.name().toLowerCase().contains(q)
                        || s.rollNo().toLowerCase().contains(q)
                        || s.department().toLowerCase().contains(q))
                .toList();
    }

    @Transactional(readOnly = true)
    public FacultyOverview facultyOverview() {
        List<AnalyticsService.Prepared> all = analytics.prepareAll();
        PredictionService.Model model = predictions.train(all);
        List<AnalyticsService.Prepared> withData = all.stream().filter(AnalyticsService.Prepared::hasData).toList();
        List<StudentSummary> summaries = withData.stream().map(p -> summary(p, model)).toList();

        double avgCgpa = summaries.stream().mapToDouble(StudentSummary::cgpa).average().orElse(0);
        double avgAtt = summaries.stream().mapToDouble(StudentSummary::attendance).average().orElse(0);
        int high = (int) summaries.stream().filter(s -> s.risk().equals("HIGH")).count();
        int medium = (int) summaries.stream().filter(s -> s.risk().equals("MEDIUM")).count();

        String[] labels = {"Below 5", "5 - 6", "6 - 7", "7 - 8", "8 - 9", "9 - 10"};
        int[] counts = new int[6];
        for (StudentSummary s : summaries) {
            int idx = s.cgpa() < 5 ? 0 : s.cgpa() < 6 ? 1 : s.cgpa() < 7 ? 2 : s.cgpa() < 8 ? 3 : s.cgpa() < 9 ? 4 : 5;
            counts[idx]++;
        }
        List<Bucket> distribution = new ArrayList<>();
        for (int i = 0; i < labels.length; i++) distribution.add(new Bucket(labels[i], counts[i]));

        Map<String, List<StudentSummary>> byDept = summaries.stream()
                .collect(Collectors.groupingBy(StudentSummary::department, TreeMap::new, Collectors.toList()));
        List<DeptAverage> departments = new ArrayList<>();
        for (Map.Entry<String, List<StudentSummary>> e : byDept.entrySet()) {
            double avg = e.getValue().stream().mapToDouble(StudentSummary::cgpa).average().orElse(0);
            departments.add(new DeptAverage(e.getKey(), Grading.round2(avg), e.getValue().size()));
        }

        Map<Integer, List<SemesterStat>> bySem = withData.stream()
                .flatMap(p -> p.stats().stream())
                .collect(Collectors.groupingBy(SemesterStat::semester, TreeMap::new, Collectors.toList()));
        List<TrendPoint> trend = new ArrayList<>();
        for (Map.Entry<Integer, List<SemesterStat>> e : bySem.entrySet()) {
            double sgpa = e.getValue().stream().mapToDouble(SemesterStat::sgpa).average().orElse(0);
            double att = e.getValue().stream().mapToDouble(SemesterStat::attendance).average().orElse(0);
            trend.add(new TrendPoint(e.getKey(), Grading.round2(sgpa), Grading.round1(att)));
        }

        List<StudentSummary> top = summaries.stream()
                .sorted(Comparator.comparingDouble(StudentSummary::cgpa).reversed())
                .limit(5).toList();
        List<StudentSummary> needsSupport = summaries.stream()
                .filter(s -> s.risk().equals("HIGH") || s.risk().equals("MEDIUM"))
                .sorted(Comparator.comparingDouble((StudentSummary s) -> s.predictedSgpa() == null ? 0 : s.predictedSgpa()))
                .limit(8).toList();

        return new FacultyOverview(all.size(), Grading.round2(avgCgpa), Grading.round1(avgAtt),
                high, medium, distribution, departments, trend, top, needsSupport);
    }

    @Transactional(readOnly = true)
    public PredictionDto whatIf(PredictRequest r) {
        PredictionService.Model model = predictions.train(analytics.prepareAll());
        return predictions.describe(model, r.studyHours(), r.attendance(), r.assignmentScore(), r.previousCgpa(), 0);
    }

    private StudentSummary summary(AnalyticsService.Prepared p, PredictionService.Model model) {
        var s = p.data().student();
        String name = s.getUser().getName();
        if (!p.hasData()) {
            return new StudentSummary(s.getId(), name, s.getRollNo(), s.getDepartment(), s.getSemester(),
                    0, 0, null, "NO_DATA", "No marks entered yet");
        }
        SemesterStat last = p.latest();
        PredictionDto prediction = predictions.forStudent(model, p);

        List<String> reasons = new ArrayList<>();
        if (last.attendance() < 75) reasons.add("Attendance " + Math.round(last.attendance()) + "%");
        if (prediction.predictedSgpa() < 7) reasons.add("Predicted SGPA " + prediction.predictedSgpa());
        if (p.cgpa() < 6) reasons.add("CGPA " + p.cgpa());

        return new StudentSummary(s.getId(), name, s.getRollNo(), s.getDepartment(), s.getSemester(),
                p.cgpa(), last.attendance(), prediction.predictedSgpa(), prediction.risk(),
                reasons.isEmpty() ? "On track" : String.join(", ", reasons));
    }
}
