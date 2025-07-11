package be.cytomine.appengine.handlers;

import java.io.File;
import java.util.UUID;

import be.cytomine.appengine.exceptions.FileStorageException;
import be.cytomine.appengine.models.task.Checksum;
import be.cytomine.appengine.models.task.Run;
import be.cytomine.appengine.repositories.ChecksumRepository;
import be.cytomine.appengine.repositories.integer.IntegerPersistenceRepository;
import be.cytomine.appengine.utils.AppEngineApplicationContext;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Component;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class StorageDataEntry
{

    private File data;

    private String name;

    private String storageId;

    private StorageDataType storageDataType;

    public StorageDataEntry(File data)
    {
        this.data = data;
    }

    public StorageDataEntry(File data, String name)
    {
        this.data = data;
        this.name = name;
    }

    public StorageDataEntry(File data, String name, StorageDataType storageDataType)
    {
        this.data = data;
        this.name = name;
        this.storageDataType = storageDataType;
    }

    public StorageDataEntry(String name, StorageDataType storageDataType)
    {
        this.name = name;
        this.storageDataType = storageDataType;
    }

    public StorageDataEntry(String name)
    {
        this.name = name;
        this.storageDataType = StorageDataType.DIRECTORY;
    }

    public StorageDataEntry(String name, String storageId)
    {
        this.name = name;
        this.storageId = storageId;
    }

    public long getChecksumCRC32(UUID uuid) throws FileStorageException
    {
        ChecksumRepository checksumRepository =
            AppEngineApplicationContext.getBean(ChecksumRepository.class);
        String reference = uuid.toString() + "-" + name;
        Checksum crc32 = checksumRepository.findByReference(reference);
        return crc32.getChecksumCRC32();
    }

    public void setChecksumCRC32(UUID uuid, long checksumCRC32)
    {
        ChecksumRepository checksumRepository = AppEngineApplicationContext.getBean(ChecksumRepository.class);
        String reference = uuid.toString() + "-" + name;
        Checksum crc32 = new Checksum(reference, checksumCRC32);

        checksumRepository.save(crc32);
    }
}
