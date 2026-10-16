import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.stream.*;
 
class SeatUnavailableException extends Exception {
  SeatUnavailableException(String msg) { super(msg); }
}
 
class Booking implements Comparable<Booking> {
  private final String name, movie;
  private final int row, col, amount;
 
  Booking(String name, String movie,
      int row, int col, int amt) {
    this.name = name; this.movie = movie;
    this.row = row; this.col = col; this.amount = amt;
  }
  String getMovie() { return movie; }
  int getRow() { return row; }
  int getCol() { return col; }
  int getAmount() { return amount; }
  public int compareTo(Booking o) {
    return Integer.compare(o.amount, amount);
  }
  public String toString() {
    return String.format("%s | %s | %c%d | Rs.%d", name,
        movie, (char) ('A' + row), col + 1, amount);
  }
}
 
public class CinemaSeatBooking {
  static final int ROWS = 5, COLS = 8;
  static final String[] MOVIES =
      {"Kalki 2898 AD", "RRR", "Jawan"};
  static Map<String, char[][]> halls = new LinkedHashMap<>();
  static List<Booking> history = new ArrayList<>();
  static int totalAmount = 0;
 
  static char[][] newHall() {
    char[][] h = new char[ROWS][COLS];
    for (int i = 0; i < ROWS; i++)
      for (int j = 0; j < COLS; j++) h[i][j] = 'A';
    return h;
  }
  static int price(int row) { return row < 2 ? 250 : 150; }
  static int price(int row, boolean student) {
    return student ? (int) (price(row) * 0.9) : price(row);
  }
  static int freeSeats(char[][] h, int row) {
    if (row == ROWS) return 0;            // base case
    int c = 0;
    for (char s : h[row]) if (s == 'A') c++;
    return c + freeSeats(h, row + 1);     // recursive case
  }
  static void displaySeats(char[][] h) {
    StringBuilder sb =
        new StringBuilder("   1 2 3 4 5 6 7 8\n");
    for (int i = 0; i < ROWS; i++) {
      sb.append((char) ('A' + i)).append("  ");
      for (int j = 0; j < COLS; j++)
        sb.append(h[i][j]).append(' ');
      sb.append('\n');
    }
    System.out.print(sb);
    System.out.println("A=Available X=Booked | Free: "
        + freeSeats(h, 0));
  }
  static String pickMovie(Scanner sc) {
    for (int i = 0; i < MOVIES.length; i++)
      System.out.printf("%d. %s%n", i + 1, MOVIES[i]);
    int n;
    do {
      System.out.print("Movie (1-" + MOVIES.length + "): ");
      n = Integer.parseInt(sc.nextLine().trim());
    } while (n < 1 || n > MOVIES.length);
    return MOVIES[n - 1];
  }
  static Booking bookSeat(String movie, String name,
      String seat, boolean student) throws SeatUnavailableException {
    String s = seat.trim().toUpperCase();
    int row = s.charAt(0) - 'A';
    int col = Integer.parseInt(s.substring(1)) - 1;
    if (row < 0 || row >= ROWS || col < 0 || col >= COLS)
      throw new SeatUnavailableException("Invalid seat " + s);
    char[][] h = halls.get(movie);
    if (h[row][col] == 'X')
      throw new SeatUnavailableException(
          s + " already booked");
    h[row][col] = 'X';
    Booking b = new Booking(name, movie, row, col,
        price(row, student));
    history.add(b);
    totalAmount += b.getAmount();
    return b;
  }
  static boolean cancelSeat(String movie, String seat) {
    String s = seat.trim().toUpperCase();
    int row = s.charAt(0) - 'A';
    int col = Integer.parseInt(s.substring(1)) - 1;
    boolean gone = history.removeIf(b ->
        b.getMovie().equals(movie)
        && b.getRow() == row && b.getCol() == col);
    if (gone) {
      halls.get(movie)[row][col] = 'A';
      totalAmount = history.stream()
          .mapToInt(Booking::getAmount).sum();
    }
    return gone;
  }
  static void showReport() {
    if (history.isEmpty()) {
      System.out.println("No bookings."); return;
    }
    history.stream().collect(Collectors.groupingBy(
        Booking::getMovie, Collectors.counting()))
        .forEach((m, n) ->
            System.out.println(m + ": " + n + " seat(s)"));
    System.out.println(history.stream().sorted()
        .map(Booking::toString)
        .collect(Collectors.joining("\n")));
    System.out.println("Revenue: Rs." + totalAmount);
  }
  static void saveReceipts() {
    List<String> lines = history.stream()
        .map(Booking::toString).collect(Collectors.toList());
    try { Files.write(Paths.get("bookings.txt"), lines); }
    catch (IOException e) {
      System.out.println("Save failed: " + e.getMessage());
    }
    finally {
      System.out.println("Saved " + lines.size() + " receipt(s).");
    }
  }
  static void viewReceipts() {
    try (BufferedReader br =
        Files.newBufferedReader(Paths.get("bookings.txt"))) {
      br.lines().forEach(System.out::println);
    } catch (IOException e) {
      System.out.println("No saved receipts yet.");
    }
  }
  public static void main(String[] args) {
    Scanner sc = new Scanner(System.in);
    for (String m : MOVIES) halls.put(m, newHall());
    int choice = 0;
    do {
      System.out.println(
          "\n1.View 2.Book 3.Cancel 4.Report 5.Receipts 6.Exit");
      System.out.print("Choice: ");
      try {
        choice = Integer.parseInt(sc.nextLine().trim());
        switch (choice) {
          case 1:
            displaySeats(halls.get(pickMovie(sc))); break;
          case 2:
            String movie = pickMovie(sc);
            displaySeats(halls.get(movie));
            System.out.print("Name: ");
            String name = sc.nextLine();
            System.out.print("Seat (e.g. B4): ");
            String seat = sc.nextLine();
            System.out.print("Student? (y/n): ");
            boolean st = sc.nextLine().equalsIgnoreCase("y");
            System.out.println("Booked: "
                + bookSeat(movie, name, seat, st));
            break;
          case 3:
            String m2 = pickMovie(sc);
            System.out.print("Seat: ");
            System.out.println(cancelSeat(m2, sc.nextLine())
                ? "Cancelled." : "Not booked.");
            break;
          case 4: showReport(); break;
          case 5: viewReceipts(); break;
          case 6:
            saveReceipts(); System.out.println("Thank you!");
            break;
          default: System.out.println("Invalid choice.");
        }
      } catch (SeatUnavailableException e) {
        System.out.println("Error: " + e.getMessage());
      } catch (NumberFormatException
          | StringIndexOutOfBoundsException e) {
        System.out.println("Invalid input.");
      }
    } while (choice != 6);
    sc.close();
  }
}
