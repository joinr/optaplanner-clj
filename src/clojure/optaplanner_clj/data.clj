(ns optaplanner-clj.data
  (:import [org.optaplanner.core.api.score.buildin.hardsoft HardSoftScore]
           [org.optaplanner.core.api.score.stream
            Constraint ConstraintProvider ConstraintFactory
            Joiners]
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
  (getId [])
  (getSubject [])
  (getTeacher [])
  (getStudentGroup [])
  (getTimeslot [])
  (setTimeslot [timeslot])
  (getRoom [])
  (setRoom [room]))

(deftype ^{PlanningEntity true} Lesson
    [^{PlanningId true :tag 'Long}
     id
     ^String subject
     ^String teacher
     ^String studentGroup
     ^{PlanningVariable {valueRangeProviderRefs ["timeslotRange"]}
       :tag 'TimeSlot :unsynchronized-mutable true}
     timeslot
     ^{PlanningVariable {valueRangeProviderRefs ["roomRange"]}
       :tag 'Room :unsynchronized-mutable true}
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
  (^java.util.List getTimeslotList    [])
  (^java.util.List getRoomList        [])
  (^java.util.List getLessonList      [])
  (^org.optaplanner.core.api.score.buildin.hardsoft.HardSoftScore getScore [])
  (setTimeslotList    [^java.util.List l])
  (setRoomList        [^java.util.List l])
  (setLessonList      [^java.util.List l])
  (setScore  [^org.optaplanner.core.api.score.buildin.hardsoft.HardSoftScore score]))

;;Problem: type tag is ignored! optaplanner complains on reflection
;;that it's not a collection or array (lessonList)

;;What if we annotate methods instead of fields?

(deftype ^{PlanningSolution true} TimeTable
    [^{:unsynchronized-mutable true} timeslotList
     ^{:unsynchronized-mutable true} roomList
     ^{:unsynchronized-mutable true} lessonList
     ^{:unsynchronized-mutable true} score]
  ITimeTable
  (^{ProblemFactCollectionProperty true
     ValueRangeProvider {id "timeslotRange"}
     :tag java.util.List}
   getTimeslotList [this] timeslotList)
  (^{ProblemFactCollectionProperty true
      ValueRangeProvider {id "roomRange"}
     :tag java.util.List}
   getRoomList     [this] roomList)
  (^{PlanningEntityCollectionProperty true
     :tag 'java.util.List}
   getLessonList   [this] lessonList)
  (^{PlanningScore true
     :tag org.optaplanner.core.api.score.buildin.hardsoft.HardSoftScore}
   getScore        [this] score)
  (setTimeslotList    [this ^java.util.List l] (do (set! timeslotList l) this))
  (setRoomList        [this ^java.util.List l] (do (set! roomList l) this))
  (setLessonList      [this ^java.util.List l] (do (set! lessonList l) this))
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
  (TimeTable. slots rooms lessons nil))

(defn ^Constraint room-conflict [^ConstraintFactory cf]
  (doto
    (.from cf Lesson)
    (.join Lesson
           (Joiners/equal    (memfn ^Lesson getTimeSlot))
           (Joiners/equal    (memfn ^Lesson getRoom))
           (.lessThan Joiners (memfn ^Lesson getId))
           )
    (.penalize "Room conflict" HardSoftScore/ONE_HARD)))


(defn ^Constraint teacher-conflict [^ConstraintFactory cf]
  (doto cf
    (.fromUniquePair Lesson
                     (Joiners/equal (memfn ^Lesson getTimeSlot))
                     (Joiners/equal (memfn ^Lesson getTeacher)))
    (.penalize "Teacher conflict" HardSoftScore/ONE_HARD)))

(defn ^Constraint student-group-conflict [^ConstraintFactory cf]
  (doto cf
    (.fromUniquePair Lesson
                     (Joiners/equal (memfn ^Lesson getTimeSlot))
                     (Joiners/equal (memfn ^Lesson getStudentGroup)))
    (.penalize "Student group conflict" HardSoftScore/ONE_HARD)))

;;optaplanner wants a damn class...
(deftype TimeTableConstraintProvider []
  ConstraintProvider
  (^"[Lorg.optaplanner.core.api.score.stream.Constraint;" defineConstraints
   [this ^ConstraintFactory cf]
   (make-array Constraint
               [(room-conflict cf)
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
