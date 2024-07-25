import java.util.*;
import java.net.URI;
import java.net.URLConnection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.concurrent.TimeUnit;
import java.io.*;

public class Main {
    public static ArrayList<String> page_queue;
    public static ArrayList<Integer> dist_queue;
    public static Set<String> seen_pages;
    public static String term_file;
    public static int seen_page_num;
    public static String search_term;
    public static boolean term_found;
    public static int thread_count;
    public static int thread_max;
    public static String seen_file;
    public static String queue_file;
    public static String dist_file;
    public static int max_pages;
    public static boolean saving;

    public static void main(String[] args) {
        seen_pages = new HashSet<String>();
        page_queue = new ArrayList<String>();
        dist_queue = new ArrayList<Integer>();
        thread_count = 0;
        thread_max = 10;
        max_pages = 10;
        saving = false;
        seen_file = "seen.txt";
        queue_file = "queue.txt";
        dist_file = "qdist.txt";
        term_file = "term.txt";

        Scanner scanner = new Scanner(System.in);
        System.out.println("Welcome to Pineapple Distance. Please enter a search term (or 'c' to continue from where you left off):");
        search_term = scanner.next();
        scanner.close();

        if ((search_term.toLowerCase()).equals("c")) {
            continueFromSave(true);
        } else {
            continueFromSave(false);
        }

        System.out.printf("Searching for: %s\n", search_term);

        String start_page = "Pineapple";
        term_found = false;

        modifyQueue(1, start_page, -1);
        manageThreads();
    }

    @SuppressWarnings("unchecked")
    private static void continueFromSave(boolean c){
        if (c) {
            try {
                FileInputStream qfis = new FileInputStream(new File(queue_file));
                FileInputStream dfis = new FileInputStream(new File(dist_file));
                FileInputStream sfis = new FileInputStream(new File(seen_file));
                ObjectInputStream qois = new ObjectInputStream(qfis);
                ObjectInputStream dois = new ObjectInputStream(dfis);
                ObjectInputStream sois = new ObjectInputStream(sfis);
                List<String> qout = (List<String>)qois.readObject();
                List<Integer> dout = (List<Integer>)dois.readObject();
                List<String> sout = (List<String>)sois.readObject();
                page_queue = new ArrayList<String>(qout);
                dist_queue = new ArrayList<Integer>(dout);
                seen_pages = new HashSet<String>(sout);
                qfis.close();
                qois.close();
                dfis.close();
                dois.close();
                sfis.close();
                sois.close();

                BufferedReader reader = new BufferedReader(new FileReader(term_file));
                search_term = reader.readLine();
                reader.close();

            } catch (FileNotFoundException e) {
                System.out.println("Save file not found. Did you try to continue without having made an initial search?");
            } catch (IOException e) {
                System.out.println("I/O error when retrieving save file. Did you try to continue after a successful search, or continue without any save data?");
            } catch (ClassNotFoundException e) {
                System.out.println("class not found error when retrieving save file");
            }
        } else {
            try {
                new PrintWriter(queue_file).close();
                new PrintWriter(dist_file).close();
                new PrintWriter(seen_file).close();
                FileWriter writer = new FileWriter(term_file, false);
                writer.write(search_term);
                writer.close();
            } catch (IOException e) {
                System.out.println("(file clear failed)");
            }
        }
    }

    private static void saveQueue() { //dont need to sync, only called from synchronized modifyQueue anyway
        saving = true;
        try {
            FileOutputStream qfos = new FileOutputStream(queue_file);
            ObjectOutputStream qoos = new ObjectOutputStream(qfos);
            List<String> saveq = page_queue;
            qoos.writeObject(saveq);
            qfos.close();
            qoos.close();

            FileOutputStream dfos = new FileOutputStream(dist_file);
            ObjectOutputStream doos = new ObjectOutputStream(dfos);
            List<Integer> saved = dist_queue;
            doos.writeObject(saved);
            dfos.close();
            doos.close();
            
        } catch (IOException e) {
            System.out.println("(I/O error: failed to save queue)");
        }
        saving = false;
    }

    private static synchronized void saveSeen() {
        saving = true;
        try {
            FileOutputStream sfos = new FileOutputStream(new File(seen_file));
            ObjectOutputStream soos = new ObjectOutputStream(sfos);
            List<String> save = new ArrayList<String>(seen_pages);
            soos.writeObject(save);
            sfos.close();
            soos.close();
        } catch (IOException e) {
            System.out.println("(seen file save failed)");
        }
        saving = false;
    }

    private static class QueueNode {
        public String title;
        public int distance;

        public QueueNode(String title, int distance) {
            this.title = title;
            this.distance = distance;
        }
    }

    // synchronized to avoid critical section problem with concurrent threads modifying the queue
    private static synchronized QueueNode modifyQueue(int pop_add, String title, int distance) {
        if (pop_add <= 0) { //pop
            String qs;
            int qi;
            if (page_queue.size() == 0) {
                qs = "";
                qi = 0;
            } else {
                qs = page_queue.get(0);
                qi = dist_queue.get(0);
                page_queue.remove(0);
                dist_queue.remove(0);
                //popFromQueue();
            }
            saveQueue();
            return new QueueNode(qs, qi);
        } else { //add
            if (page_queue.size() > 100) {
                page_queue.remove(0);
                dist_queue.remove(0);
            }
            page_queue.add(title);
            dist_queue.add(distance);
            saveQueue();
        }
        return null;
    }

    private static void manageThreads() {
        Thread t = new Thread();
        while (seen_page_num < max_pages && !term_found) {
            try {
                TimeUnit.SECONDS.sleep(1);
            } catch (InterruptedException e) {
                System.out.println("(wait interrupted)");
            }
            QueueNode node = modifyQueue(0, null, 0);
            node.distance += 1;
            t = startThread(node);
        }
        if (seen_page_num >= max_pages) {
            System.out.println("Searched 10 pages, but could not find your term!");
            System.out.println("Waiting for threads, please hold...");
            try {
                t.join();
            } catch (InterruptedException e) {
                System.out.println("interrupted");
            }
            System.out.println("To continue searching another batch, run the program again and enter 'c' as your search term.");
            quitProgram(1);
        }
    }

    private static Thread startThread(final QueueNode node) {
        do {} while (thread_count >= thread_max);
        Thread thread = new Thread(() -> threadFunc(node));
        thread_count++;
        thread.start();
        return thread;
    }

    // retrieve HTML from given url
    private static String getPage(String title) {
        if (haveSeenPage(title) || title == null || title.equals("")) {
            return "";
        }
        String prefix = "https://en.wikipedia.org/wiki/";
        String url = prefix.concat(title);
        seen_pages.add(title);
        seen_page_num++;
        saveSeen();
        String data = null;
        URLConnection connection = null;
        
        try {
            URI uri =  new URI(url);
            connection = uri.toURL().openConnection();
            Scanner scanner = new Scanner(connection.getInputStream());
            scanner.useDelimiter("\\Z");
            data = scanner.next();
            scanner.close();
            System.out.printf("Viewing page %s\n", title.replaceAll("_", " "));
        } catch (Exception e) {
            //e.printStackTrace();
            System.out.printf("Error accessing page %s\n", title);
        }
        return data;
    }

    private static boolean hasSearchTerm(String data) {
        Pattern pattern = Pattern.compile("(?<=_|\\b)" + search_term + "(?=_|\\b)", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(data);
        if(matcher.find()) {
            return true;
        }
        return false;
    }

    // gets all URLs on a given page
    private static void getLinks(String html, int distance) {
        //ArrayList<String> url_list = new ArrayList<String>();
        if (html == null) {
            return;
        }
        Pattern pattern = Pattern.compile("href=\"/wiki/([^:\\?#]*?)\"");
        // matches all relative (ie en.wikipedia.org) links on the page, excluding those with ?, :, # (usually anchors & help pages)
        Matcher m = pattern.matcher(html);
        String thisMatch;
        String page_title;
        if(m.find()) {
            do {
                thisMatch = m.group(0);
                page_title = thisMatch.substring(12,thisMatch.length()-1);
                if (!seen_pages.contains(page_title)) {
                    modifyQueue(1, page_title, distance);
                }
            } while (m.find(m.start()+1));
        }
        //return url_list;
    }

    private static boolean haveSeenPage(String title) {
        return seen_pages.contains(title);
    }

    private static void threadFunc(QueueNode node) {
        String content = getPage(node.title);
        if (hasSearchTerm(content)) {
            System.out.println("Term found, stopping threads...");
            term_found = true;
            try {
                TimeUnit.SECONDS.sleep(2);
            } catch (InterruptedException e) {
                System.out.println("(wait interrupted)");
            }
            System.out.printf("Found %s on page %s, %d click(s) away from Pineapple.\n", search_term, node.title.replaceAll("_", " "), node.distance);
            quitProgram(0);
        }
        getLinks(content, node.distance);
        thread_count--;
    }

    private static void quitProgram(int status) {
        if (status == 0) {
            Thread thread = new Thread(() -> quitProgramSuccessThread());
            thread.start();
        } else {
            Thread thread = new Thread(() -> quitProgramThread());
            thread.start();
        }
    }

    private static void quitProgramThread() {
        System.out.println("Saving & quitting... (please do not manually quit or data may not save correctly)");
        try {
            TimeUnit.SECONDS.sleep(10);
        } catch (InterruptedException e) {
            System.out.println("(wait interrupted)");
        }
        System.exit(0);
    }

    private static void quitProgramSuccessThread() {
        System.out.println("Clearing data & quitting...");
        try {
            new PrintWriter(queue_file).close();
            new PrintWriter(dist_file).close();
            new PrintWriter(seen_file).close();
            new PrintWriter(term_file).close();
        } catch (IOException e) {
            System.out.println("(file clear failed)");
        }
        try {
            TimeUnit.SECONDS.sleep(10);
        } catch (InterruptedException e) {
            System.out.println("(wait interrupted)");
        }
        System.exit(0);
    }

}