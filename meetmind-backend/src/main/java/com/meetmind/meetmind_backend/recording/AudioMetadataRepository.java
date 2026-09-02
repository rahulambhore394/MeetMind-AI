package com.meetmind.meetmind_backend.recording;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AudioMetadataRepository extends JpaRepository<AudioMetadata, Long> {
}
