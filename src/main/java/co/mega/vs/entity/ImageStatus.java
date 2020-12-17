package co.mega.vs.entity;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

public class ImageStatus {

    private String fileName;

    private AtomicInteger status; //0:未下载， 1:已下载

    ReentrantLock lock = new ReentrantLock();

    public ImageStatus(String fileName, AtomicInteger status) {
        this.fileName = fileName;
        this.status = status;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public AtomicInteger getStatus() {
        return status;
    }

    public void setStatus(AtomicInteger status) {
        this.status = status;
    }

    public ReentrantLock getLock() {
        return lock;
    }
}
