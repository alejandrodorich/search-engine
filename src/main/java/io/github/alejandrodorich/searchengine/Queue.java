package io.github.alejandrodorich.searchengine;

import java.util.NoSuchElementException;
import java.util.Set;
import java.util.HashSet;

/**
 * Store and manage URLs that have to be crawled during the crawling process. 
 * Additionally, ensure that no URL is crawled more than once.
 */
class Queue {

    private Entry header = new Entry(null, null, null);
    private int size;
    private Set<String> visitedUrls = new HashSet<>();
    
    private class Entry {
        String url;
        Entry next;
        Entry previous;
    
        public Entry (String url, Entry next, Entry previous) {
            this.url = url;
            this.next = next;
            this.previous = previous;
        }
    }

    /**
     * Construct an empty queue.
     */
    public Queue() {
        header.next = header;
        header.previous = header;
    }

     /**
     * Return the number of elements in the queue.
     * 
     * @return  The number of URLs currently in the queue.
     */
    public int getSize() {
        return size;
    }

     /**
     * Add a new URL to the queue, as long as it has never been stored in the queue.
     * 
     * @param url  The URL to be added to the queue.
     */
    public void add(String url) {
        if (!contains(url)) {
            Entry newEntry = new Entry(url, header, header.previous);
            header.previous.next = newEntry;
            header.previous = newEntry;
            size++;
        }
    }
    
    /**
     * Remove and return the first URL in the queue, marking it as processed.
     * 
     * @return  The first URL in the queue.
     * @throws NoSuchElementException   If the queue is empty.
     */
    public String poll() {
        if (size == 0) throw new NoSuchElementException();
        String nextUrl = header.next.url;
        Entry secondEntry = header.next.next;
        secondEntry.previous = header;
        header.next = secondEntry;
        size--;
        visitedUrls.add(nextUrl);
        return nextUrl;
    }

    // Check if the given URL has already been processed.
    private boolean contains(String url) {
        return (duplicateInQueue(url) || visitedUrls.contains(url)); 
    }

    // Check if the given URL is currently stored in the queue.
    private boolean duplicateInQueue(String url) {
        if (size == 0) return false;
        boolean duplicate = false;
        Entry temp = header;
        while ((temp.next != header) && (!duplicate)) {
            temp = temp.next;
            if (temp.url.equals(url)) {
                duplicate = true;
            }
        }
        return duplicate;
    } 
}
