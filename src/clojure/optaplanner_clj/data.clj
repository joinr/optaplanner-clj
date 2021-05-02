(ns optaplanner-clj.data
  (:import [org.optaplanner.core.api.score.buildin.hardsoft HardSoftScore]
           [org.optaplanner.core.api.score.stream
            Constraint ConstraintProvider ConstraintFactory
            Joiners]
           [java.time DayOfWeek LocalTime]))

(definterface INamed
  (^String getName []))

(deftype TimeSlot [^DayOfWeek  dayOfWeek ^LocalTime startTime ^LocalTime endTime]
  Object
  (toString [this] (str dayOfWeek " " startTime)))

(defn ->TimeSlot [day starttime endtime]
  (TimeSlot. day starttime endtime))


(deftype Room [^String name]
  INamed
  (getName [this] name)
  Object
  (toString [this] name))

(defn ->Room [x] (Room. (str x)))

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

(defn ->Lesson [id course teacher student]
  (Lesson. id course teacher student nil nil))


;; Timetable
(definterface ITimeTable
  (getTimeslotList [])
  (getRoomList [])
  (getLessonList [])
  (getScore []))


(deftype ^{PlanningSolution true} TimeTable
    [^{ProblemFactCollectionProperty true
       ValueRangeProvider "timeslotRange"
       :tag 'List} timeslotList

     ^{ProblemFactCollectionProperty true
       ValueRangeProvider "roomRange"
       :tag 'List}  roomList

     ^{ProblemFactCollectionProperty true
       :tag 'List } lessonList

     ^{PlanningScore true
       :tag 'HardSoftScore} score]
  ITimeTable
  (getTimeslotList [this] timeslotList)
  (getRoomList [this] roomList)
  (getLessonList [this] lessonList)
  (getScore [this] score))

(defn ->TimeTable [slots rooms lessons]
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
