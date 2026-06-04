package com.supremeai.controller;

import com.supremeai.model.VPNConnection;
import com.supremeai.repository.VPNRepository;
import com.supremeai.response.ApiResponse;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/admin/vpn")
public class VPNController extends BaseAdminController<VPNConnection, String> {

  private final VPNRepository vpnRepository;

  public VPNController(VPNRepository vpnRepository) {
    this.vpnRepository = vpnRepository;
  }

  @GetMapping
  public Mono<ResponseEntity<ApiResponse<Map<String, Object>>>> getConnections() {
    return wrapList(vpnRepository.findAll(), "connections");
  }

  @PostMapping
  public Mono<ResponseEntity<ApiResponse<Map<String, Object>>>> createConnection(
      @RequestBody VPNConnection connection) {
    return wrapSave(vpnRepository.save(connection), "VPN connection created");
  }

  @DeleteMapping("/{id}")
  public Mono<ResponseEntity<ApiResponse<String>>> deleteConnection(@PathVariable String id) {
    return wrapDelete(vpnRepository.deleteById(id), "VPN connection deleted");
  }
}
