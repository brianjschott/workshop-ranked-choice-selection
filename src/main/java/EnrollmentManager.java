import java.io.FileReader;
import java.io.FileWriter;
import java.util.*;

import com.opencsv.*;

//class will contain methods for importing attendance and workshop data
public class EnrollmentManager {

    private static ArrayList<Workshop> workshopList;
    private ArrayList<Attendee> attendeeList;
    private ArrayList<Attendee> scheduledAttendees;
    private ArrayList<Attendee> leftovers;

    public void printAttendees() {
        System.out.println(attendeeList);
    }

    public ArrayList<Attendee> getLeftovers() {
        return leftovers;
    }

    public EnrollmentManager() {
        workshopList = new ArrayList<Workshop>();
        attendeeList = new ArrayList<Attendee>();
        scheduledAttendees = new ArrayList<Attendee>();
        leftovers = new ArrayList<Attendee>();
    }

    public static ArrayList<Workshop> getFreeTalkSessions() {
        ArrayList<Workshop> freeTalks = new ArrayList<Workshop>();
        for (Workshop w: workshopList) {
            if (w.isFreeTalk()) { freeTalks.add(w); }
        }
        return freeTalks;
    }

    //gets lowest attended workshop from the list
    public static Workshop getLowestAttendedWorkshop(char session) {
        int smallest = 400;
        Workshop smallestWorkshop = null;
        for (Workshop w: workshopList) {
            DoubleSessionWorkshop d = (DoubleSessionWorkshop) w;
            //get session type attendance, compare with smallest
            int numAttendees = d.getNumberOfAttendees(session);
            //if smallest, update smallest variable and smallestWorkshop
            if (numAttendees < smallest) {
                smallest = numAttendees;
                smallestWorkshop = d;
            }
        }
        return smallestWorkshop;
    }

    public static Workshop getLowestAttendedWorkshop(ArrayList<Workshop> workshops, char session) {
        int smallest = 400;
        Workshop smallestWorkshop = null;
        for (Workshop w: workshops) {
            DoubleSessionWorkshop d = (DoubleSessionWorkshop) w;
            //get session type attendance, compare with smallest
            int numAttendees = d.getNumberOfAttendees(session);
            //if smallest, update smallest variable and smallestWorkshop
            if (numAttendees < smallest) {
                smallest = numAttendees;
                smallestWorkshop = d;
            }
        }
        return smallestWorkshop;

    }

    //Only sorts DoubleSession workshops by comparing number of attendees
    public static void sortWorkshopListByAttendance(ArrayList<Workshop> listToBeSorted, char session) {
        listToBeSorted.sort(Comparator.comparingInt(o -> ((DoubleSessionWorkshop) o).getNumberOfAttendees(session)));
    }

    //method takes in a filepath, imports the file into a List of String arrays,
    // (one for each row), and changes each into a list of Workshops and Attendees
    //Workshops will utilize the WorkshopFactory
    public void importData(String workshopCSVFilePath, String attendeeCSVFilePath) {
        List<String[]> workshopData = getListFromCSV(workshopCSVFilePath);
        List<String[]> attendeeData = getListFromCSV(attendeeCSVFilePath);

        //loop through workshopData; instantiate objects for workshopList
        initializeWorkshopList(workshopData);

        //loop through attendeeData, instantiate objects for each attendee
        initializeAttendeeList(attendeeData);

    }

    //important: CSV files should use **TAB SEPARATED VALUES** to avoid issues with commas in Workshop descriptions and lists of moderators/presenters
    public List<String[]> getListFromCSV(String filepath) {
        try {
            final CSVParser parser = new CSVParserBuilder()
                    .withSeparator('\t')
                    .build();
            final CSVReader reader = new CSVReaderBuilder(new FileReader(filepath))
                    .withSkipLines(0)
                    .withCSVParser(parser)
                    .build();
            return reader.readAll();
        } catch (Exception e) { System.out.println("Error reading in file."); }

        return null;
    }

    /*
    workshop spreadsheet format should be:
        Column A: ID
        Column B: Name
        Column C: Description
        Column D: URL
        Column E, Faculty Moderators (separated by commas)
        Column F, Presenters (separated by commas)
        Column G, Max Attendance
        Column H, Sessions (either A, B, or AB)
        Column I, Is Free Talk (can accommodate students who did not pick it)
     */
    public void initializeWorkshopList(List<String[]> workshopData) {
        WorkshopFactory w = new WorkshopFactory();

        //change comma-separated strings for moderators/presenters into array of Strings
        for (String[] workshopRow: workshopData) {
            workshopList.add(w.makeWorkshop(
                    Integer.parseInt(workshopRow[0]),
                    workshopRow[1],
                    workshopRow[2],
                    workshopRow[3],
                    workshopRow[4],
                    workshopRow[5],
                    Integer.parseInt(workshopRow[6]),
                    workshopRow[7].toUpperCase(),
                    Boolean.parseBoolean(workshopRow[8])
            ));
        }
    }

    /* Data should be organized as follows:
    Column A: Name
    Column B: Grade
    Column C: Email Address
    Columns D-H: 1st-5th preferences

     */
    public void initializeAttendeeList(List<String[]> attendeeData) {
        for (String[] attendeeRow: attendeeData) {
            attendeeList.add(new Attendee(
                    attendeeRow[0],
                    Integer.parseInt(attendeeRow[1]),
                    attendeeRow[2],
                    new String[] {attendeeRow[3], attendeeRow[4], attendeeRow[5], attendeeRow[6], attendeeRow[7]}
            ));
        }
    }

    /*
    iterate through each Workshop
    for each workshop, form a list of attendees that have their first preference to be that workshop (may
    match by IDs)

    push to Workshop
     */
    public void selectWorkshopPreferencesForAttendees() {
        //for each preference level
        for (int preferenceLevel = 1; preferenceLevel <= 5; preferenceLevel++) {
            //for each workshop, place each preference level in turn
            for (Workshop workshop : workshopList) {
                //get the attendees who listed this workshop as the one they wanted
                //match the name they put for the first item in attendance.workshopPreferences to the
                //name of the given workshop
                ArrayList<Attendee> attendees;

                if (workshop instanceof SingleSessionWorkshop) {
                    SingleSessionWorkshop s = (SingleSessionWorkshop) workshop;
                    attendees = getListOfAttendeesByPreference(s, preferenceLevel);
                    for (int j = s.getNumberOfAttendees(); j < s.getMaxAttendance(); j++) {
                        if (attendees.size() > 0) {
                            Attendee randomAttendee = attendees.get(new Random().nextInt(attendees.size()));
                            s.addAttendee(randomAttendee);
                            randomAttendee.setWorkshop(workshop, s.getSession());
                            attendees.remove(randomAttendee);
                            //if scheduled for both sessions, remove the attendee
                            if (!randomAttendee.isAvailable()) {
                                scheduledAttendees.add(randomAttendee);
                            }
                        }
                    }
                } else if (workshop instanceof DoubleSessionWorkshop) {
                    DoubleSessionWorkshop d = (DoubleSessionWorkshop) workshop;
                    //first handle Session A
                    attendees = getListOfAttendeesByPreference(d, preferenceLevel, 'A');
                    for (int j = d.getNumberOfAttendees('A'); j < d.getMaxAttendance(); j++) {
                        if (attendees.size() > 0) {
                            Attendee randomAttendee = attendees.get(new Random().nextInt(attendees.size()));
                            d.addAttendee(randomAttendee, 'A');
                            randomAttendee.setWorkshop(workshop, 'A');
                            attendees.remove(randomAttendee);
                            if (!randomAttendee.isAvailable()) {
                                scheduledAttendees.add(randomAttendee);                            }
                        }
                    }

                    //then Session B
                    attendees = getListOfAttendeesByPreference(d, preferenceLevel, 'B');
                    for (int j = d.getNumberOfAttendees('B'); j < d.getMaxAttendance(); j++) {
                        if (attendees.size() > 0) {
                            Attendee randomAttendee = attendees.get(new Random().nextInt(attendees.size()));
                            d.addAttendee(randomAttendee, 'B');
                            randomAttendee.setWorkshop(workshop, 'B');
                            attendees.remove(randomAttendee);
                            if (!randomAttendee.isAvailable()) {
                                scheduledAttendees.add(randomAttendee);                            }
                        }
                    }
                }

            }
        }

        //gets individuals who got scheduled for no workshops,
        // i.e. aren't present in the scheduledAttendees list
        //get entire list of attendees, check each of them to see if they have an opening
        //use the method isAvailable to check for this

        for (Attendee person: attendeeList) {
            if (person.isAvailable()) {
                leftovers.add(person);
            }
        }

        //place students in available workshops
        //three groups: students only available for workshopA, students only for workshopB, and students
        //with no placements

        ArrayList<Attendee> availableWorkshopA = new ArrayList<Attendee>();
        ArrayList<Attendee> availableWorkshopB = new ArrayList<Attendee>();
        ArrayList<Attendee> availableWorkshopAandB = new ArrayList<Attendee>();

        for (Attendee person: leftovers) {
            if (person.getWorkshopA() == null && person.getWorkshopB() == null) {
                availableWorkshopAandB.add(person);
            }
            else if (person.getWorkshopA() == null) {
                availableWorkshopA.add(person);
            }
            else if (person.getWorkshopB() == null) {
                availableWorkshopB.add(person);
            }
        }

        //System.out.println(availableWorkshopA.size());
        //System.out.println(availableWorkshopB.size());
        //System.out.println(availableWorkshopAandB.size());
        System.out.println(leftovers.size());

        //NOTE: THIS ONLY WORKS FOR DOUBLE SESSION WORKSHOPS BECAUSE OF TIME CONSTRAINTS
        //ADD IN SINGLE SESSION SUPPORT LATER
        //first, sort the list of freeTalks
        //for students open for workshopA only, add them to the lowest attended free workshop session for session A
        //if they are already in that workshop, then put them in the second-most attended workshop
        ArrayList<Workshop> freeTalks = getFreeTalkSessions();
        for (Attendee person: availableWorkshopA) {
            sortWorkshopListByAttendance(freeTalks, 'A');
            DoubleSessionWorkshop lowest = (DoubleSessionWorkshop) freeTalks.get(0);
            DoubleSessionWorkshop secondLowest = (DoubleSessionWorkshop) freeTalks.get(1);
            if (person.getWorkshopB().equals(lowest)) {
                secondLowest.addAttendee(person, 'A');
                person.setWorkshop(secondLowest, 'A');
            }
            else {
                lowest.addAttendee(person, 'A');
                person.setWorkshop(lowest, 'A');
            }

            leftovers.remove(person);
            scheduledAttendees.add(person);
        }

        for (Attendee person: availableWorkshopB) {
            sortWorkshopListByAttendance(freeTalks, 'B');
            DoubleSessionWorkshop lowest = (DoubleSessionWorkshop) freeTalks.get(0);
            DoubleSessionWorkshop secondLowest = (DoubleSessionWorkshop) freeTalks.get(1);
            if (person.getWorkshopA().equals(lowest)) {
                secondLowest.addAttendee(person, 'B');
                person.setWorkshop(secondLowest, 'B');
            }
            else {
                lowest.addAttendee(person, 'B');
                person.setWorkshop(lowest, 'B');
            }

            leftovers.remove(person);
            scheduledAttendees.add(person);
        }

        for (Attendee person: availableWorkshopAandB) {
            sortWorkshopListByAttendance(freeTalks, 'B');
            DoubleSessionWorkshop lowest = (DoubleSessionWorkshop) freeTalks.get(0);
            DoubleSessionWorkshop secondLowest = (DoubleSessionWorkshop) freeTalks.get(1);
            lowest.addAttendee(person, 'A');
            person.setWorkshop(lowest, 'A');
            secondLowest.addAttendee(person, 'B');
            person.setWorkshop(secondLowest, 'B');
            leftovers.remove(person);
            scheduledAttendees.add(person);
        }

        System.out.println("Number of LeftOvers: " + leftovers.size());
    }

    //parameter: a Workshop and a pref num that is from 1 - 5
    //return: ArrayList of attendees that listed that specific workshopID at that specific preference #
    //Attendees must also be open during that timeslot in order to be added to that list
    public ArrayList<Attendee> getListOfAttendeesByPreference(SingleSessionWorkshop workshop, int prefNum) {
        prefNum--; //for array index
        String workshopName = workshop.getName();
        ArrayList<Attendee> tempList = new ArrayList<Attendee>();

        for (Attendee attendee : this.attendeeList){
            String preference = attendee.getWorkshopPreferences()[prefNum];
            //check first to see if the name matches
            if (preference.equals(workshopName)) {
                //if single session, check if student is available for that session
                if (workshop instanceof SingleSessionWorkshop) {
                    SingleSessionWorkshop s = (SingleSessionWorkshop) workshop;
                    if (s.getSession() == 'A' && attendee.getWorkshopA() == null) {
                        tempList.add(attendee);
                    } else if (s.getSession() == 'B' && attendee.getWorkshopB() == null) {
                        tempList.add(attendee);
                    }
                }
            }
        }
        return tempList;
    }

    //overloaded version of above method that works for Double Session workshops, which
    //helps to avoid issues of double booking individuals for both Session A and Session B
    public ArrayList<Attendee> getListOfAttendeesByPreference(DoubleSessionWorkshop workshop, int prefNum, char session) {
        prefNum--; //for array index
        String workshopName = workshop.getName();
        ArrayList<Attendee> tempList = new ArrayList<Attendee>();

        for (Attendee attendee : this.attendeeList){
            String preference = attendee.getWorkshopPreferences()[prefNum];
            //check first to see if the name matches
            if (preference.equals(workshopName)) {
                if (workshop instanceof DoubleSessionWorkshop) {
                    //ensures they are available and they aren't already booked for the same workshop in another session
                    if (attendee.getWorkshopA() == null && session == 'A' && attendee.getWorkshopB() != workshop) {
                        tempList.add(attendee);
                    }
                    else if (attendee.getWorkshopB() == null && session == 'B' && attendee.getWorkshopA() != workshop) {
                        tempList.add(attendee);
                    }
                }
            }
        }
        return tempList;
    }

    public Workshop getWorkshopByName(String name) {
        for (Workshop workshop : workshopList) {
            if (workshop.getName().equals(name))
                return workshop;
        }
        return null;
    }

    public void printWorkshopChoices() {
        for (Workshop w : workshopList) {
            System.out.println(w);
        }
    }

    //Attendee data should be turned into a spreadsheet
    //The following columns will be used:
    //Column A: Email Address
    //Column B: Attendee Name
    //Column C: Attendee Grade
    //Column D: Attendee's Workshop A name and URL
    //Column E: Attendee's Workshop B name and URL
    public void convertAttendeeDataToCSV() throws Exception {
        List<String[]> attendeeFinalData = new ArrayList<String[]>();

        for (Attendee attendee : scheduledAttendees) {
            String workshopAInfo = "None", workshopBInfo = "None";
            if (attendee.getWorkshopA() != null) {
                workshopAInfo = attendee.getWorkshopA().getName() + " (" + attendee.getWorkshopA().getUrl() + ")";
            }
            if (attendee.getWorkshopB() != null) {
                workshopBInfo = attendee.getWorkshopB().getName() + " (" + attendee.getWorkshopB().getUrl() + ")";

            }
            String[] dataRow = {
                    attendee.getEmailAddress(),
                    attendee.getName(),
                    Integer.toString(attendee.getGrade()),
                    workshopAInfo,
                    workshopBInfo
            };
            attendeeFinalData.add(dataRow);

        }

        //convert list of Attendees to a list of Strings
        String filePath = "data/attendeeresults.tsv";
        FileWriter writer = new FileWriter(filePath);
        CSVParser parser = new CSVParserBuilder().build();
        ICSVWriter csvParserWriter = new CSVWriterBuilder(writer)
                .withParser(parser)
                .withLineEnd(ICSVWriter.RFC4180_LINE_END)
                .build(); // will produce a CSVParserWriter

        ICSVWriter csvWriter = new CSVWriterBuilder(writer)
                .withSeparator('\t')
                .withQuoteChar(CSVWriter.NO_QUOTE_CHARACTER)
                .build(); // will produce a CSVWriter


        csvWriter.writeAll(attendeeFinalData);
        csvWriter.close();
    }

    //Workshop data should be turned into a spreadsheet for attendance purposes
    //Two separate columns should occur for both A and B sessions
    //The following columns will be used:
    //Column A: Email Address of Moderator
    //Column B: Workshop Name
    //Column C: Workshop URL
    //Column D: Presenter
    //Column E: Workshop Session A Attendees String
    //Column F: Workshop Session A Attendance #
    //Column G: Workshop Session B Attendees String
    //Column H: Workshop Session B Attendance #
    public void convertWorkshopDataToCSV() throws Exception {
        List<String[]> workshopFinalData = new ArrayList<String[]>();

        for (Workshop workshop : workshopList) {
            String workshopA = "";
            String workshopB = "";
            int attendanceA = 0, attendanceB = 0;
            if (workshop instanceof SingleSessionWorkshop) {
                SingleSessionWorkshop s = (SingleSessionWorkshop) workshop;
                ArrayList<Attendee> attendeeList = s.getAttendees();
                if (s.getSession() == 'A') {
                    attendanceA = attendeeList.size();
                }
                else {
                    attendanceB = attendeeList.size();
                }
                for (Attendee a : attendeeList) {
                    if (s.getSession() == 'A') {
                        workshopA += a.getName();
                        workshopA += ";";
                    }
                    else if (s.getSession() == 'B') {
                        workshopB += a.getName();
                        workshopB += ";";
                    }
                    else {
                        System.out.println("Something went wrong...");

                    }
                }
            }
            else if (workshop instanceof DoubleSessionWorkshop) {
                DoubleSessionWorkshop d = (DoubleSessionWorkshop) workshop;
                ArrayList<Attendee> attendeeListA = d.getAttendees('A');
                ArrayList<Attendee> attendeeListB = d.getAttendees('B');
                attendanceA = attendeeListA.size();
                attendanceB = attendeeListB.size();
                for (Attendee a : attendeeListA) {
                    workshopA += a.getName();
                    workshopA += ";";
                }
                for (Attendee a : attendeeListB) {
                    workshopB += a.getName();
                    workshopB += ";";
                }

            }
            String[] dataRow = {
                    workshop.getModerators(),
                    workshop.getName(),
                    workshop.getUrl(),
                    workshop.getPresenters(),
                    workshopA,
                    String.valueOf(attendanceA),
                    workshopB,
                    String.valueOf(attendanceB)
            };
            workshopFinalData.add(dataRow);

        }

        //convert list of Attendees to a list of Strings
        String filePath = "data/workshopresults.tsv";
        FileWriter writer = new FileWriter(filePath);
        CSVParser parser = new CSVParserBuilder().build();
        ICSVWriter csvParserWriter = new CSVWriterBuilder(writer)
                .withParser(parser)
                .withLineEnd(ICSVWriter.RFC4180_LINE_END)
                .build(); // will produce a CSVParserWriter

        ICSVWriter csvWriter = new CSVWriterBuilder(writer)
                .withSeparator('\t')
                .withQuoteChar(CSVWriter.NO_QUOTE_CHARACTER)
                .build(); // will produce a CSVWriter


        csvWriter.writeAll(workshopFinalData);
        csvWriter.close();
    }

    //Attendee data should be turned into a spreadsheet
    //The following columns will be used:
    //Column A: Email Address
    //Column B: Attendee Name
    //Column C: Attendee Grade
    //Column D: Attendee's Workshop A name and URL
    //Column E: Attendee's Workshop B name and URL
    public void convertLeftoverDataToCSV() throws Exception {
        List<String[]> attendeeFinalData = new ArrayList<String[]>();

        for (Attendee attendee : leftovers) {
            String workshopAInfo = "None", workshopBInfo = "None";
            if (attendee.getWorkshopA() != null) {
                workshopAInfo = attendee.getWorkshopA().getName() + " (" + attendee.getWorkshopA().getUrl() + ")";
            }
            if (attendee.getWorkshopB() != null) {
                workshopBInfo = attendee.getWorkshopB().getName() + " (" + attendee.getWorkshopB().getUrl() + ")";

            }
            String[] dataRow = {
                    attendee.getEmailAddress(),
                    attendee.getName(),
                    Integer.toString(attendee.getGrade()),
                    workshopAInfo,
                    workshopBInfo
            };
            attendeeFinalData.add(dataRow);

        }

        //convert list of Attendees to a list of Strings
        String filePath = "data/leftovers.tsv";
        FileWriter writer = new FileWriter(filePath);
        CSVParser parser = new CSVParserBuilder().build();
        ICSVWriter csvParserWriter = new CSVWriterBuilder(writer)
                .withParser(parser)
                .withLineEnd(ICSVWriter.RFC4180_LINE_END)
                .build(); // will produce a CSVParserWriter

        ICSVWriter csvWriter = new CSVWriterBuilder(writer)
                .withSeparator('\t')
                .withQuoteChar(CSVWriter.NO_QUOTE_CHARACTER)
                .build(); // will produce a CSVWriter


        csvWriter.writeAll(attendeeFinalData);
        csvWriter.close();
    }
}

