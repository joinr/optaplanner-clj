(ns optaplanner-clj.core
  (:require [optaplanner-clj.data :as data])
  (:import [optaplanner_clj.data Lesson TimeTable TimeTableConstraintProvider]
           [java.time DayOfWeek LocalTime]
           [java.util UUID Collections]
           [org.optaplanner.core.api.solver SolverManager SolverFactory]
           [org.optaplanner.core.config.solver SolverConfig]
           [org.optaplanner.core.config.solver.termination TerminationConfig]
           [org.optaplanner.core.config.score.director ScoreDirectorFactoryConfig]))

(defn ->problem []
  (let [ts (for [[l r] (partition 2 1 (range 8 16))]
             (data/->time-slot  (DayOfWeek/MONDAY) (LocalTime/of l 30) (LocalTime/of r 30)))
        rs  (mapv data/->room ["Room A" "Room B" "Room C"])
        ls (mapv (fn [[id course teacher grade]] (data/->lesson id course teacher grade))
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
    (data/->time-table ts rs ls)))

(defn ->config []
  (.withTerminationConfig
   (doto (SolverConfig.)
     (.setSolutionClass TimeTable)
     (.setEntityClassList (Collections/singletonList Lesson))
     (.setScoreDirectorFactoryConfig
      (doto (ScoreDirectorFactoryConfig.)
        (.setConstraintProviderClass TimeTableConstraintProvider))))
   (doto (TerminationConfig.)
     (.setSecondsSpentLimit 10))))

(defn ^SolverManager ->solver-manager []
  (-> (->config)
      SolverFactory/create
      SolverManager/create))

(defn solve
  ([problem]
   (let [sm        (->solver-manager)
         problemId (UUID/randomUUID)
         ^TimeTable
         solution (.getFinalBestSolution
                   (.solve sm problemId problem))]
     [(mapv (fn [^Lesson l]
              [(.getId l)
               (.getSubject l)
               (.getTeacher l)
               (.getStudentGroup l)
               (str  (.getTimeslot l))
               (str  (.getRoom l))])
            (.getLessonList solution))
      (.getScore solution)]))
  ([] (solve (->problem))))

(comment
  (solve (->problem))
  )
