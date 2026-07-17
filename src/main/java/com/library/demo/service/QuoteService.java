package com.library.demo.service;

import com.library.demo.model.Quote;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Service
public class QuoteService {
    private final List<Quote> quotes = new ArrayList<>();
    private final Random random = new Random();
    private int lastQuoteIndex = -1;

    public QuoteService() {
        // Initialize 100 reading/books related quotes
        quotes.add(new Quote("A reader lives a thousand lives before he dies.", "George R.R. Martin"));
        quotes.add(new Quote("Books are uniquely portable magic.", "Stephen King"));
        quotes.add(new Quote("Today a reader, tomorrow a leader.", "Margaret Fuller"));
        quotes.add(new Quote("Reading is dreaming with open eyes.", "Anesa Kajtazovic"));
        quotes.add(new Quote("Knowledge begins with curiosity.", "Unknown"));
        quotes.add(new Quote("A book is a dream that you hold in your hands.", "Neil Gaiman"));
        quotes.add(new Quote("The more that you read, the more things you will know.", "Dr. Seuss"));
        quotes.add(new Quote("Books are the quietest and most constant friends.", "Charles William Eliot"));
        quotes.add(new Quote("Reading is to the mind what exercise is to the body.", "Richard Steele"));
        quotes.add(new Quote("I have always imagined that Paradise will be a kind of library.", "Jorge Luis Borges"));
        quotes.add(new Quote("Show me a family of readers, and I will show you the people who move the world.", "Napoleon Bonaparte"));
        quotes.add(new Quote("There is no frigate like a book to take us lands away.", "Emily Dickinson"));
        quotes.add(new Quote("A room without books is like a body without a soul.", "Marcus Tullius Cicero"));
        quotes.add(new Quote("The reading of all good books is like a conversation with the finest minds.", "René Descartes"));
        quotes.add(new Quote("To read is to fly: it is to soar to a point of vantage.", "A.C. Grayling"));
        quotes.add(new Quote("Books are a uniquely portable magic.", "Stephen King"));
        quotes.add(new Quote("We read to know we are not alone.", "C.S. Lewis"));
        quotes.add(new Quote("A house without books is like a room without windows.", "Heinrich Mann"));
        quotes.add(new Quote("Outside of a dog, a book is a man's best friend. Inside of a dog it's too dark to read.", "Groucho Marx"));
        quotes.add(new Quote("There are worse crimes than burning books. One of them is not reading them.", "Joseph Brodsky"));
        quotes.add(new Quote("You can never get a cup of tea large enough or a book long enough to suit me.", "C.S. Lewis"));
        quotes.add(new Quote("Reading is a discount ticket to everywhere.", "Mary Schmich"));
        quotes.add(new Quote("If you only read the books that everyone else is reading, you can only think what everyone else is thinking.", "Haruki Murakami"));
        quotes.add(new Quote("No two persons ever read the same book.", "Edmund Wilson"));
        quotes.add(new Quote("Rainy days should be spent at home with a cup of tea and a good book.", "Bill Watterson"));
        quotes.add(new Quote("Books serve to show a man that those original thoughts of his aren't very new after all.", "Abraham Lincoln"));
        quotes.add(new Quote("The library is inhabited by spirits that come out of the pages at night.", "Harlan Coben"));
        quotes.add(new Quote("Libraries will get you through times of no money better than money will get you through times of no libraries.", "Anne Herbert"));
        quotes.add(new Quote("Reading brings us unknown friends.", "Honoré de Balzac"));
        quotes.add(new Quote("Some books leave us free and some books make us free.", "Ralph Waldo Emerson"));
        quotes.add(new Quote("A great book should leave you with many experiences, and slightly exhausted at the end.", "William Styron"));
        quotes.add(new Quote("Sleep is good, he said, and books are better.", "George R.R. Martin"));
        quotes.add(new Quote("That is the good thing about this world... there are always more books.", "Elizabeth McCracken"));
        quotes.add(new Quote("One path, indeed, there is which leads to happiness: the cultivation of the mind.", "William Hazlitt"));
        quotes.add(new Quote("Books make great gifts because they have whole worlds inside of them.", "Neil Gaiman"));
        quotes.add(new Quote("Libraries are reservoirs of strength, grace and wit, mind and letter.", "Germaine Greer"));
        quotes.add(new Quote("Once you learn to read, you will be forever free.", "Frederick Douglass"));
        quotes.add(new Quote("The journey of a lifetime starts with the turning of a page.", "Rachel Anders"));
        quotes.add(new Quote("Reading is a passport to countless adventures.", "Unknown"));
        quotes.add(new Quote("A book is like a garden carried in the pocket.", "Chinese Proverb"));
        quotes.add(new Quote("There is no friend as loyal as a book.", "Ernest Hemingway"));
        quotes.add(new Quote("To acquire the habit of reading is to construct for yourself a refuge from almost all the miseries of life.", "W. Somerset Maugham"));
        quotes.add(new Quote("I read a book one day and my whole life was changed.", "Orhan Pamuk"));
        quotes.add(new Quote("If you don’t like to read, you haven’t found the right book.", "J.K. Rowling"));
        quotes.add(new Quote("Reading gives us someplace to go when we have to stay where we are.", "Mason Cooley"));
        quotes.add(new Quote("Books are the mirrors of the soul.", "Virginia Woolf"));
        quotes.add(new Quote("The world belongs to those who read.", "Rick Holland"));
        quotes.add(new Quote("Children are made readers on the laps of their parents.", "Emilie Buchwald"));
        quotes.add(new Quote("A book is a device to ignite the imagination.", "Alan Bennett"));
        quotes.add(new Quote("Books are the quietest of friends; they are the most accessible and wisest of counselors.", "Charles W. Eliot"));
        quotes.add(new Quote("Libraries are the wardrobe of the human mind.", "John Dyer"));
        quotes.add(new Quote("To read is to empower, to discover, and to grow.", "Unknown"));
        quotes.add(new Quote("Read the best books first, or you may not have a chance to read them at all.", "Henry David Thoreau"));
        quotes.add(new Quote("The art of reading is in great part that of acquiring a better understanding of life.", "André Maurois"));
        quotes.add(new Quote("We are of opinion that instead of letting books grow moldy behind iron grates, it is better to let them wear out in being read.", "Jules Verne"));
        quotes.add(new Quote("Literature is the safe and traditional way of sharing human experiences.", "Isaac Bashevis Singer"));
        quotes.add(new Quote("Keep reading. It's one of the most marvelous adventures anyone can have.", "Lloyd Alexander"));
        quotes.add(new Quote("A library is not a luxury but one of the necessities of life.", "Henry Ward Beecher"));
        quotes.add(new Quote("Good books, like good friends, are few and chosen; the more select, the more enjoyable.", "Louisa May Alcott"));
        quotes.add(new Quote("Reading is the ultimate escape and the best way to explore other minds.", "Unknown"));
        quotes.add(new Quote("The books that the world calls immoral are books that show the world its own shame.", "Oscar Wilde"));
        quotes.add(new Quote("The only important thing in a book is the meaning that it has for you.", "W. Somerset Maugham"));
        quotes.add(new Quote("Learning is a treasure that will follow its owner everywhere.", "Chinese Proverb"));
        quotes.add(new Quote("Books have a unique way of stopping time.", "Dave Eggers"));
        quotes.add(new Quote("Reading is an active, imaginative act; it takes work.", "Khaled Hosseini"));
        quotes.add(new Quote("A library implies an act of faith.", "Victor Hugo"));
        quotes.add(new Quote("No entertainment is so cheap as reading, nor any pleasure so lasting.", "Mary Wortley Montagu"));
        quotes.add(new Quote("Books are a refuge from the harsh realities of life.", "Unknown"));
        quotes.add(new Quote("Every book you pick up has its own lesson.", "Kiran Desai"));
        quotes.add(new Quote("A book must be the axe for the frozen sea within us.", "Franz Kafka"));
        quotes.add(new Quote("There is no barrier, lock, or bolt that you can set upon the freedom of my mind.", "Virginia Woolf"));
        quotes.add(new Quote("Reading makes all other learning possible.", "Barack Obama"));
        quotes.add(new Quote("Books are key to the chambers of the past and the gates of the future.", "Unknown"));
        quotes.add(new Quote("Fill your house with books, in all the crannies and all the nooks.", "Dr. Seuss"));
        quotes.add(new Quote("To think is easy. To act is difficult. To act as one's thoughts is the most difficult thing.", "Johann Wolfgang von Goethe"));
        quotes.add(new Quote("Books are the compasses and telescopes and sextants and charts which other men have prepared to help us navigate the dangerous seas of human life.", "Jesse Lee Bennett"));
        quotes.add(new Quote("My alma mater was books, a good library.", "Malcolm X"));
        quotes.add(new Quote("Libraries: the medicine chest of the soul.", "Greek Library Inscription"));
        quotes.add(new Quote("To live in libraries is to live in the hearts of great thinkers.", "Unknown"));
        quotes.add(new Quote("The paper is patient, but the reader is critical.", "Dutch Saying"));
        quotes.add(new Quote("A book is a gift you can open again and again.", "Garrison Keillor"));
        quotes.add(new Quote("Reading is the beacon that guides us through intellectual darkness.", "Unknown"));
        quotes.add(new Quote("One must always be careful of books, and what is inside them, for words have the power to change us.", "Cassandra Clare"));
        quotes.add(new Quote("A book is a version of the world. If you don't like it, ignore it; or write your own.", "Salman Rushdie"));
        quotes.add(new Quote("A public library is the most democratic thing in the world.", "Andrew Carnegie"));
        quotes.add(new Quote("Only the very weak-minded refuse to be influenced by literature.", "Oscar Wilde"));
        quotes.add(new Quote("To read is to step into a new life.", "Unknown"));
        quotes.add(new Quote("An investment in knowledge pays the best interest.", "Benjamin Franklin"));
        quotes.add(new Quote("The man who does not read has no advantage over the man who cannot read.", "Mark Twain"));
        quotes.add(new Quote("Think before you speak. Read before you think.", "Fran Lebowitz"));
        quotes.add(new Quote("A book is a master key to the hidden chambers of knowledge.", "Unknown"));
        quotes.add(new Quote("So many books, so little time.", "Frank Zappa"));
        quotes.add(new Quote("What a blessing it is to love books.", "Elizabeth von Arnim"));
        quotes.add(new Quote("Read. Read. Read. Just don't read one type of book.", "F. Scott Fitzgerald"));
        quotes.add(new Quote("The ink of the scholar is more holy than the blood of the martyr.", "Prophet Muhammad"));
        quotes.add(new Quote("Books are the heritage of humanity.", "Unknown"));
        quotes.add(new Quote("We are what we read.", "Joseph Brodsky"));
        quotes.add(new Quote("A truly great book should be read in youth, again in maturity and once more in old age.", "Robertson Davies"));
        quotes.add(new Quote("Read, read, read. Read everything.", "William Faulkner"));
        quotes.add(new Quote("A library is a delivery room for the birth of ideas.", "Norman Cousins"));
    }

    public synchronized Quote getRandomQuote() {
        if (quotes.isEmpty()) return null;
        if (quotes.size() == 1) return quotes.get(0);

        int index;
        do {
            index = random.nextInt(quotes.size());
        } while (index == lastQuoteIndex);

        lastQuoteIndex = index;
        return quotes.get(index);
    }
}
