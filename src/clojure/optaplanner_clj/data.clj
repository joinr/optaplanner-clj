(ns optaplanner-clj.data
  (:require [optaplanner-clj.util :refer [method-ref]])
  (:import [org.optaplanner.core.api.score.buildin.hardsoft HardSoftScore]
           [org.optaplanner.core.api.score.stream
            Constraint ConstraintProvider ConstraintFactory
            Joiners]
           [org.optaplanner.core.api.domain.solution.cloner SolutionCloner]
           [org.optaplanner.core.api.domain.solution PlanningSolution]
           [org.optaplanner.core.api.domain.variable PlanningVariable]
           [org.optaplanner.core.api.domain.entity PlanningEntity]
           [org.optaplanner.core.api.domain.lookup PlanningId]
           [org.optaplanner.core.api.domain.solution PlanningEntityCollectionProperty]
           [org.optaplanner.core.api.domain.solution PlanningScore]
           [org.optaplanner.core.api.domain.solution PlanningSolution]
           [org.optaplanner.core.api.domain.solution ProblemFactCollectionProperty]
           [org.optaplanner.core.api.domain.valuerange ValueRangeProvider]
           [java.time DayOfWeek LocalTime]
           [java.util List]))

(defprotocol ISolution
  (clone-solution [this]))

(def cloner
  (reify SolutionCloner
    (cloneSolution [this original]
      (clone-solution original))))

(def +clone-class+ (type cloner))
(definterface INamed
  (^String getName []))

(deftype TimeSlot [^DayOfWeek  dayOfWeek ^LocalTime startTime ^LocalTime endTime]
  Object
  (toString [this] (str dayOfWeek " " startTime)))

(defn ->time-slot [day starttime endtime]
  (TimeSlot. day starttime endtime))


(deftype Room [^String name]
  INamed
  (getName [this] name)
  Object
  (toString [this] name))

(defn ->room [x] (Room. (str x)))

(definterface ILesson
  (^long getId [])
  (^String getSubject [])
  (^String getTeacher [])
  (^String getStudentGroup [])
  (^optaplanner_clj.data.TimeSlot getTimeslot [])
  (setTimeslot [^optaplanner_clj.data.TimeSlot timeslot])
  (^optaplanner_clj.data.Room getRoom [])
  (setRoom [^optaplanner_clj.data.Room room]))

(deftype ^{PlanningEntity true} Lesson
    [^{PlanningId true :tag long}
     id
     ^String subject
     ^String teacher
     ^String studentGroup
     ^{PlanningVariable {valueRangeProviderRefs ["timeslotRange"]}
       :tag 'optaplanner_clj.data.TimeSlot :unsynchronized-mutable true}
     timeslot
     ^{PlanningVariable {valueRangeProviderRefs ["roomRange"]}
       :tag 'optaplanner_clj.data.Room :unsynchronized-mutable true}
     room]
  ILesson
  (getId [this] id)
  (getSubject [this] subject)
  (getTeacher [this] teacher)
  (getStudentGroup [this] studentGroup)
  (getTimeslot [this] timeslot)
  (setTimeslot [this t] (set! timeslot t))
  (getRoom [this] room)
  (setRoom [this r] (set! room r))

  Object
  (toString [this] (str id)))

(defn ->lesson [id course teacher student]
  (Lesson. id course teacher student nil nil))


;; The solutionClass (class optaplanner_clj.data.TimeTable) has a
;; PlanningEntityCollectionProperty annotated member (public final java.lang.Object
;; optaplanner_clj.data.TimeTable.lessonList) that does not return a Collection or
;; an array.

;; Timetable
(definterface ITimeTable
  (^"[Loptaplanner_clj.data.TimeSlot;" getTimeslotList    [])
  (^"[Loptaplanner_clj.data.Room;" getRoomList        [])
  (^"[Loptaplanner_clj.data.Lesson;" getLessonList      [])
  (^org.optaplanner.core.api.score.buildin.hardsoft.HardSoftScore getScore [])
  (setTimeslotList    [^"[Loptaplanner_clj.data.TimeSlot;" l])
  (setRoomList        [^"[Loptaplanner_clj.data.Room;" l])
  (setLessonList      [^"[Loptaplanner_clj.data.Lesson;" l])
  (setScore  [^org.optaplanner.core.api.score.buildin.hardsoft.HardSoftScore score]))

;;Problem: type tag is ignored! optaplanner complains on reflection
;;that it's not a collection or array (lessonList)

;;What if we annotate methods instead of fields?

(deftype ^{PlanningSolution {solutionCloner +clone-class+}}
    TimeTable
    [^{:unsynchronized-mutable true} timeslotList
     ^{:unsynchronized-mutable true} roomList
     ^{:unsynchronized-mutable true} lessonList
     ^{:unsynchronized-mutable true} score]
  ISolution
  (clone-solution [this]
    (TimeTable. (aclone ^"[Loptaplanner_clj.data.TimeSlot;" timeslotList)
                (aclone ^"[Loptaplanner_clj.data.Room;" roomList)
                (aclone ^"[Loptaplanner_clj.data.Lesson;" lessonList)
                score))
  ITimeTable
  (^{ProblemFactCollectionProperty true
     ValueRangeProvider {id "timeslotRange"}
     :tag "[Loptaplanner_clj.data.TimeSlot;"}
   getTimeslotList [this] timeslotList)
  (^{ProblemFactCollectionProperty true
      ValueRangeProvider {id "roomRange"}
     :tag "[Loptaplanner_clj.data.Room;"}
   getRoomList     [this] roomList)
  (^{PlanningEntityCollectionProperty true
     :tag "[Loptaplanner_clj.data.Lesson;"}
   getLessonList   [this] lessonList)
  (^{PlanningScore true
     :tag org.optaplanner.core.api.score.buildin.hardsoft.HardSoftScore}
   getScore        [this] score)
  (setTimeslotList    [this ^"[Loptaplanner_clj.data.TimeSlot;" l] (do (set! timeslotList l) this))
  (setRoomList        [this ^"[Loptaplanner_clj.data.Room;" l] (do (set! roomList l) this))
  (setLessonList      [this ^"[Loptaplanner_clj.data.Lesson;" l] (do (set! lessonList l) this))
  (setScore           [this ^org.optaplanner.core.api.score.buildin.hardsoft.HardSoftScore s]
    (do (set! score s) this)))

#_
(deftype ^{PlanningSolution true} TimeTable
    [^{ProblemFactCollectionProperty true
       ValueRangeProvider "timeslotRange"
       :tag java.util.List} timeslotList

     ^{ProblemFactCollectionProperty true
       ValueRangeProvider "roomRange"
       :tag java.util.List}  roomList

     ^{PlanningEntityCollectionProperty true
       :tag 'java.util.List} lessonList

     ^{PlanningScore true
       :tag org.optaplanner.core.api.score.buildin.hardsoft.HardSoftScore}  score]
  ITimeTable
  (getTimeslotList [this] timeslotList)
  (getRoomList     [this] roomList)
  (getLessonList   [this] lessonList)
  (getScore        [this] score))

(defn ->time-table [slots rooms lessons]
  (TimeTable. (into-array TimeSlot slots)
              (into-array Room rooms)
              (into-array Lesson lessons) nil))

(defn ^Constraint room-conflict [^ConstraintFactory cf]
  (-> (.from cf Lesson)
      (.join Lesson
             (Joiners/equal    (method-ref ^Lesson getTimeslot))
             (Joiners/equal    (method-ref ^Lesson getRoom))
             (Joiners/lessThan (method-ref ^Lesson getId))
             )
      (.penalize "Room conflict" HardSoftScore/ONE_HARD)))


(defn ^Constraint teacher-conflict [^ConstraintFactory cf]
  (-> cf
    (.fromUniquePair Lesson
                     (Joiners/equal (method-ref ^Lesson getTimeslot))
                     (Joiners/equal (method-ref ^Lesson getTeacher)))
    (.penalize "Teacher conflict" HardSoftScore/ONE_HARD)))

(defn ^Constraint student-group-conflict [^ConstraintFactory cf]
  (-> cf
    (.fromUniquePair Lesson
                     (Joiners/equal (method-ref ^Lesson getTimeslot))
                     (Joiners/equal (method-ref ^Lesson getStudentGroup)))
    (.penalize "Student group conflict" HardSoftScore/ONE_HARD)))

;;optaplanner wants a damn class...
(deftype TimeTableConstraintProvider []
  ConstraintProvider
  (^"[Lorg.optaplanner.core.api.score.stream.Constraint;" defineConstraints
   [this ^ConstraintFactory cf]
   (into-array Constraint   [(room-conflict cf)
                             (teacher-conflict cf)
                             (student-group-conflict cf)])))

#_
(defn ^ConstraintProvider ->make-provider []
  (reify ConstraintProvider
    (^"[Lorg.optaplanner.core.api.score.stream.Constraint;" defineConstraints
     [this ^ConstraintFactory cf]
     (make-array Constraint
                 [(room-conflict cf)
                  (teacher-conflict cf)
                  (student-group-conflict cf)]))))
