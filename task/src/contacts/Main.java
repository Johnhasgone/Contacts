package contacts;
import java.io.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Main {
    public static void main(String[] args) throws IOException, ClassNotFoundException {

        Scanner userInput = new Scanner(System.in);
        String action;

        String filename = "phonebook.db";
        Contacts phoneBook = new Contacts(); //(Contacts) SerializationUtils.deserialize(filename);
        CurrentState currentState = new CurrentState();

        while (true) {
            currentState.askingUserAction();
            action = userInput.nextLine();
            if (action.equals("exit")) { break; }
            switch (action) {
                case "add":
                    phoneBook.addRecord();
                    try {
                        SerializationUtils.serialize(phoneBook, filename);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    break;
                case "list":
                    currentState.setCurrentState("list");
                    phoneBook.printContacts();
                    break;
                case "search":
                    phoneBook.searchRecords();
                    break;
                case "count":
                    phoneBook.countRecords();
                    break;
                case "menu":
                    currentState.setCurrentState("menu");
                    currentState.searchList = null;
                    break;
                case "edit":
                    currentState.getCurrentRecord().editRecord();
                    break;
                case "back":
                    currentState.setCurrentState("menu");
                    break;
                case "delete":
                    phoneBook.removeRecord(currentState.getCurrentRecord());
                    currentState.setCurrentRecord(null);
                case "again":
                    break;
                default:
                    if (action.matches("[0-9]+")) {
                        if (currentState.getCurrentState().equals("search")) {
                            currentState.setCurrentRecord(currentState.searchList.get(Integer.parseInt(action) - 1));
                        } else {
                            currentState.setCurrentRecord(phoneBook.records.get(Integer.parseInt(action) - 1));
                        }
                        currentState.setCurrentState("record");
                        currentState.getCurrentRecord().printInfo();
                }
            }
            System.out.println();
        }
    }
}

class CurrentState {
    /**
     * Implements different submenus for main menu: search, list, record
     */
    final String[] states = {"menu", "search", "list", "record"};
    private String currentState;
    private Record currentRecord = null;
    ArrayList<Record> searchList = null;

    CurrentState() {
        currentState = states[0];
    }

    public void setCurrentRecord(Record currentRecord) {
        this.currentRecord = currentRecord;
    }

    public Record getCurrentRecord() {
        return currentRecord;
    }

    public void setCurrentState(String currentState) {
        this.currentState = currentState;
    }

    public String getCurrentState() {
        return currentState;
    }

    public void askingUserAction() {
        switch (currentState) {
            case "menu":
                System.out.print("[menu] Enter action (add, list, search, count, exit: ");
                break;
            case "search":
                System.out.print("[search] Enter action ([number], back, again): ");
                break;
            case "list":
                System.out.print("[list] Enter action ([number], back): ");
                break;
            case "record":
                System.out.print("[record] Enter action (edit, delete, menu): ");
        }
    }

}

class SerializationUtils {
    /**
     * Serialize the given object to the file
     */
    public static void serialize(Object obj, String fileName) throws IOException {
        FileOutputStream fos = new FileOutputStream(fileName);
        BufferedOutputStream bos = new BufferedOutputStream(fos);
        ObjectOutputStream oos = new ObjectOutputStream(bos);
        oos.writeObject(obj);
        oos.close();
    }

    /**
     * Deserialize to an object from the file
     */
    public static Object deserialize(String fileName) throws IOException, ClassNotFoundException {
        FileInputStream fis = new FileInputStream(fileName);
        BufferedInputStream bis = new BufferedInputStream(fis);
        ObjectInputStream ois = new ObjectInputStream(bis);
        Object obj = ois.readObject();
        ois.close();
        return obj;
    }
}

 class Contacts implements Serializable {
    ArrayList<Record> records = new ArrayList<>();

    transient Scanner scanner = new Scanner(System.in);

    Contacts() {
    }

     private void readObject(ObjectInputStream ois) throws Exception {
         ois.defaultReadObject();
         scanner = new Scanner(System.in);
     }

     public void addRecord() {
        System.out.print("Enter the type (person, organization): ");
        String type = scanner.nextLine();

        Record record = "person".equals(type) ? new Person() : new Organization();

        String[] fields = record.getFields().split(", ");
        for (String field: fields) {
            System.out.printf("Enter the %s: ", field);
            String value = scanner.nextLine();
            record.setField(field, value);
        }
        this.records.add(record);
        System.out.println("The record added.");

    }

    public void removeRecord(Record record) {
        for (Record rec: records) {
            if (rec == record) {
                this.records.remove(rec);
                break;
            }
        }
        System.out.println("The record removed!");
    }



    public void countRecords() {
        System.out.printf("The Phone Book has %d records.\n", this.records.size());
    }

    public void printContacts() {
        int counter = 1;
        for (Record item : this.records) {
            System.out.print(counter + ". " + item.getShortInfo() + "\n");
            counter++;
        }
    }

    public void getInfo() {
        if (this.records.size() == 0) {
            System.out.println("No records to get info!");
            return ;
        }
        printContacts();
        System.out.print("Enter index to show info: ");
        int recNum = scanner.nextInt() - 1;
        this.records.get(recNum).printInfo();
    }

     public void searchRecords() {
        System.out.print("Enter search query: ");
        String query = "(?i).*" + scanner.nextLine() + ".*";
        Pattern pattern = Pattern.compile(query);
        ArrayList <Record> searchResult = new ArrayList<>();
        for (Record record: records) {
            if (pattern.matcher(record.getFullInfo()).matches()) {
                searchResult.add(record);
            }
        }
        System.out.printf("Found %d result%s: \n", searchResult.size(), searchResult.size() > 1 ? "s" : "");
        int count = 1;
        for (Record record: searchResult) {
            System.out.printf("%d. %s\n", count, record.getShortInfo());
        }
     }
 }

abstract class Record implements Serializable {

    private LocalDateTime creationDate;
    private LocalDateTime editionDate;
    private String number = "";
    final private String template =
            "^(?i)([+](\\w )?([(]?\\w{2,}[)]?)?|[(]\\w{2,}[)]|\\w{1,}|\\w{2,}[ -][(]\\w{2,}[)])([ -][0-9a-z]{2,})?([ -][0-9a-z]{2,})?([ -][0-9a-z]{2,})?$";
    Pattern pattern = Pattern.compile(template);

    Record() {
        creationDate = LocalDateTime.now().withSecond(0).withNano(0);
        editionDate = LocalDateTime.now().withSecond(0).withNano(0);
    }

    public void setCreationDate() {
        this.creationDate = LocalDateTime.now().withSecond(0).withNano(0);
    }

    public LocalDateTime getCreationDate() {
        return this.creationDate;
    }

    public void setEditionDate() {
        this.editionDate = LocalDateTime.now().withSecond(0).withNano(0);
    }

    public LocalDateTime getEditionDate() {
        return this.editionDate;
    }

    private boolean checkNumber(String number) {
        Matcher matcher = pattern.matcher(number);
        return matcher.matches();
    }

    public void setNumber (String number) {
        if (checkNumber(number)) {
            this.number = number;
        } else {
            this.number = "[no number]";
            System.out.println("Wrong number format!");
        }
    }

    public void editRecord() {
        Scanner scanner = new Scanner(System.in);
        System.out.printf("Select a field (%s): ", this.getFields());
        String field = scanner.nextLine();
        System.out.printf("Enter %s: ", field);
        String value = scanner.nextLine();
        this.setField(field, value);
        this.setEditionDate();
        System.out.println("Saved");
        this.printInfo();
    }

    public String getNumber() {
        return this.number;
    }

    public boolean hasNumber() {
        return !this.getNumber().equals("");
    }

    abstract public String getShortInfo();

    abstract public String getFullInfo();

    abstract public void printInfo();

    abstract public String getFields();

    abstract public void setField(String field, String value);

    abstract public String getField(String field);

}

class Person extends Record {
    private String name;
    private String surname;
    private String birthDate;
    private String gender;

    public Person() {
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return this.name;
    }

    public void setSurname(String surname) {
        this.surname = surname;
    }

    public String getSurname() {
        return this.surname;
    }

    public void setBirthDate(String birthDate) {
        if (birthDate == null || birthDate.equals("")) {
            System.out.println("Bad birth date!");
            this.birthDate = "[no data]";
        } else {
            this.birthDate = birthDate;
        }
    }

    public String getBirthDate() {
        return this.birthDate;
    }

    public void setGender(String gender) {
        if (gender.equals("M") || gender.equals("F")) {
            this.gender = gender;
        } else {
            System.out.println("Bad gender!");
            this.gender = "[no data]";
        }
    }

    public String getGender() {
        return this.gender;
    }

    @Override
    public String getShortInfo() {
        return getName()+ " " + getSurname();
    }

    @Override
    public String getFullInfo() {
        return getName()+ " " + getSurname() + " " + getBirthDate() + " " + getGender() + " " + getNumber();
    }

    @Override
    public void printInfo() {
        System.out.println("Name: " + this.getName());
        System.out.println("Surname: " + this.getSurname());
        System.out.println("Birth date: " + this.getBirthDate());
        System.out.println("Gender: " + this.getGender());
        System.out.println("Number: " + this.getNumber());
        System.out.println("Time created: " + this.getCreationDate());
        System.out.println("Time last edit: " + this.getEditionDate());
    }

    @Override
    public String getFields() {
        return "name, surname, birth, gender, number";
    }

    @Override
    public void setField(String field, String value) {
        switch (field) {
            case "name":
                setName(value);
                break;
            case "surname":
                setSurname(value);
                break;
            case "birth":
                setBirthDate(value);
                break;
            case "gender":
                setGender(value);
                break;
            case "number":
                setNumber(value);
                break;
        }
    }

    @Override
    public String getField(String field) {
        switch (field) {
            case "name":
                return getName();
            case "surname":
                return getSurname();
            case "birth":
                return getBirthDate();
            case "gender":
                return getGender();
            case "number":
                return getNumber();
            default:
                return getName();
        }
    }
}

class Organization extends Record {
    private String organizationName;
    private String address;

    public Organization() {
    }

    public void setOrganizationName(String organizationName) {
        this.organizationName = organizationName;
    }

    public String getOrganizationName() {
        return this.organizationName;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getAddress() {
        return this.address;
    }

    @Override
    public String getShortInfo() {
        return getOrganizationName();
    }

    @Override
    public String getFullInfo() {
        return getOrganizationName()+ " " + getAddress() + " " + getNumber();
    }

    @Override
    public void printInfo() {
        System.out.println("Organization name: " + this.getOrganizationName());
        System.out.println("Address: " + this.getAddress());
        System.out.println("Number: " + this.getNumber());
        System.out.println("Time created: " + this.getCreationDate());
        System.out.println("Time last edit: " + this.getEditionDate());
    }

    @Override
    public String getFields() {
        return "name, address, number";
    }

    @Override
    public void setField(String field, String value) {
        switch (field) {
            case "name":
                setOrganizationName(value);
                break;
            case "address":
                setAddress(value);
                break;
            case "number":
                setNumber(value);
                break;
        }
    }

    @Override
    public String getField(String field) {
        switch (field) {
            case "name":
                return getOrganizationName();
            case "address":
                return getAddress();
            case "number":
                return getNumber();
            default:
                return getOrganizationName();
        }
    }
}