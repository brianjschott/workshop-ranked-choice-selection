import java.util.ArrayList;

public class Main {
    public static void main(String[] args) {
        final int NUMBER_OF_SIMULATIONS = 50;
        ArrayList<EnrollmentManager> enrollments = new ArrayList<EnrollmentManager>();
        for (int i = 0; i < NUMBER_OF_SIMULATIONS; i++) {
            EnrollmentManager e = new EnrollmentManager();
            e.importData("data/workshops.tsv", "data/attendees.tsv"); //file names here
            e.selectWorkshopPreferencesForAttendees();
            //e.printWorkshopChoices();
            enrollments.add(e);
        }

        int smallest = getSmallestStandardDeviation(NUMBER_OF_SIMULATIONS, enrollments);

        try {
            EnrollmentManager e = enrollments.get(smallest);
            e.convertAttendeeDataToCSV();
            e.convertWorkshopDataToCSV();
            e.convertLeftoverDataToCSV();
        } catch (Exception exception) {
            System.out.println(exception);
            exception.printStackTrace();
        }
    }

    private static int getSmallestStandardDeviation(int NUMBER_OF_SIMULATIONS, ArrayList<EnrollmentManager> enrollments) {
        double[] enrollmentStandardDeviations = new double[NUMBER_OF_SIMULATIONS];

        for (int i = 0; i < enrollmentStandardDeviations.length; i++) {
            enrollmentStandardDeviations[i] = enrollments.get(i).calculateStandardDeviationOfAttendance('A') + enrollments.get(i).calculateStandardDeviationOfAttendance('B');
        }
        int smallest = indexOfSmallest(enrollmentStandardDeviations);
        System.out.println("Smallest I could find is " + enrollmentStandardDeviations[smallest] + " standard deviations.");
        return smallest;
    }

    //stolen from StackExchange!
    public static int indexOfSmallest(double[] array){

        // add this
        if (array.length == 0)
            return -1;

        int index = 0;
        double min = array[index];

        for (int i = 1; i < array.length; i++){
            if (array[i] <= min){
                min = array[i];
                index = i;
            }
        }
        return index;
    }
}
