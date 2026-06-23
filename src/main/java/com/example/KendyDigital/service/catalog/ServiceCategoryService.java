package com.example.KendyDigital.service.catalog;

import com.example.KendyDigital.dto.catalog.request.CreateServiceCategoryRequest;
import com.example.KendyDigital.dto.catalog.request.UpdateServiceCategoryRequest;
import com.example.KendyDigital.dto.catalog.response.ServiceCategoryResponse;
import java.util.List;

public interface ServiceCategoryService {
    List<ServiceCategoryResponse> listAll();
    List<ServiceCategoryResponse> listForAdmin(Integer page, Integer size);
    List<ServiceCategoryResponse> listRoot();
    ServiceCategoryResponse getById(Long id);
    ServiceCategoryResponse getBySlug(String slug);
    ServiceCategoryResponse create(Long adminUserId, CreateServiceCategoryRequest request);
    ServiceCategoryResponse update(Long adminUserId, Long id, UpdateServiceCategoryRequest request);
    void delete(Long adminUserId, Long id);
}
