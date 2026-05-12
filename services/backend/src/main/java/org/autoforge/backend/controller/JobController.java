package org.autoforge.backend.controller;

import jakarta.validation.Valid;
import org.autoforge.backend.dto.CreateJobRequest;
import org.autoforge.backend.dto.JobResponse;
import org.autoforge.backend.service.JobService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/jobs")
public class JobController {

  private final JobService jobService;

  public JobController(JobService jobService) {
    this.jobService = jobService;
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public JobResponse createJob(@Valid @RequestBody CreateJobRequest request) {
    return jobService.createJob(request);
  }
}
