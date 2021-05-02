(ns optaplanner-clj.core
  (:require [optaplanner-clj.data :as data])
  (:import [optaplanner_clj.data TimeTableConstraintProvider]
           [java.time DayOfWeek LocalTime]
           [java.util UUID Collections]
           [org.optaplanner.core.api.solver SolverManager SolverFactory]
           [org.optaplanner.core.config.solver SolverConfig]
           [org.optaplanner.core.config.solver.termination TerminationConfig]
           [org.optaplanner.core.config.score.director ScoreDirectorFactoryConfig]))

(defn ->problem []
  (let [ts (for [[l r] (partition 2 1 (range 8 16))]
             (optaplanner_clj.data.Timeslot.  (DayOfWeek/MONDAY) (LocalTime/of l 30) (LocalTime/of r 30)))
        rs  (mapv #(Room. %) ["Room A" "Room B" "Room C"])
        ls (mapv (fn [[id course teacher grade]] (data/->Lesson id course teacher grade))
                 [[101 "Math" "B. May" "9th grade"]
                  [102 "Physics" "M. Curie" "9th grade"]
                  [103 "Geography" "M. Polo" "9th grade"]
                  [104 "English" "I. Jones" "9th grade"]
                  [105 "Spanish" "P. Cruz" "9th grade"]
                  [201 "Math" "B. May" "10th grade"]
                  [202 "Chemistry" "M. Curie" "10th grade"]
                  [203 "History" "I. Jones" "10th grade"]
                  [204 "English" "P. Cruz" "10th grade"]
                  [205 "French" "M. Curie" "10th grade"]])]
     (data/->TimeTable ts rs ls)))

(defn solve [problem]
  (let [sm (SolverManager/create
             (SolverFactory/create
               (.withTerminationConfig
                 (doto (SolverConfig.)
                   (.setSolutionClass TimeTable)
                   (.setEntityClassList (Collections/singletonList Lesson))
                   (.setScoreDirectorFactoryConfig
                     (doto (ScoreDirectorFactoryConfig.)
                       (.setConstraintProviderClass TimeTableConstraintProvider))))
                 (doto (TerminationConfig.)
                   (.setSecondsSpentLimit 10)))))
        problemId (UUID/randomUUID)
        solution (.getFinalBestSolution
                  (.solve sm problemId problem))]
    
    (list (map #(vector (.getId %)
                        ;; (.getSubject %)
                        ;; (.getTeacher %)
                        ;; (.getStudentGroup %)
                        (.toString (.getTimeslot %))
                        (.toString (.getRoom %)))
               (.getLessonList solution))
          (.getScore solution))))


(comment
  (solve (generateProblem))
  )
