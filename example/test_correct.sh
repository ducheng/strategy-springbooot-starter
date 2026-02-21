#!/bin/bash

echo "========================================="
echo "完整接口测试验证 (13个测试用例 - 修正版)"
echo "========================================="
echo ""

# Test 1: GET /users/1 without header (should use oldUserService)
echo "测试 1: GET /api/users/1 (无请求头 -> old service)"
result1=$(curl -s -w "\n%{http_code}" http://localhost:8084/api/users/1)
code1=$(echo "$result1" | tail -n1)
body1=$(echo "$result1" | head -n-1)
echo "  HTTP $code1: $body1"
echo ""

# Test 2: GET /users/1 with version=v2 (should use newUserService)
echo "测试 2: GET /api/users/1 (version=v2 -> new service)"
result2=$(curl -s -w "\n%{http_code}" -H "version: v2" http://localhost:8084/api/users/1)
code2=$(echo "$result2" | tail -n1)
body2=$(echo "$result2" | head -n-1)
echo "  HTTP $code2: $body2"
echo ""

# Test 3: GET /users/1/info without param (should use oldUserService)
echo "测试 3: GET /api/users/1/info (无参数 -> old service)"
result3=$(curl -s -w "\n%{http_code}" http://localhost:8084/api/users/1/info)
code3=$(echo "$result3" | tail -n1)
body3=$(echo "$result3" | head -n-1)
echo "  HTTP $code3: $body3"
echo ""

# Test 4: GET /users/1/info with useNew=true (should use newUserService)
echo "测试 4: GET /api/users/1/info?useNew=true (-> new service)"
result4=$(curl -s -w "\n%{http_code}" "http://localhost:8084/api/users/1/info?useNew=true")
code4=$(echo "$result4" | tail -n1)
body4=$(echo "$result4" | head -n-1)
echo "  HTTP $code4: $body4"
echo ""

# Test 5: GET /users/route/1 where ID<=100 (should use oldUserService)
echo "测试 5: GET /api/users/route/1 (ID<=100 -> old service)"
result5=$(curl -s -w "\n%{http_code}" http://localhost:8084/api/users/route/1)
code5=$(echo "$result5" | tail -n1)
body5=$(echo "$result5" | head -n-1)
echo "  HTTP $code5: $body5"
echo ""

# Test 6: GET /users/route/101 where ID>100 (should use newUserService, but no data)
echo "测试 6: GET /api/users/route/101 (ID>100 -> new service, 无数据)"
result6=$(curl -s -w "\n%{http_code}" http://localhost:8084/api/users/route/101)
code6=$(echo "$result6" | tail -n1)
body6=$(echo "$result6" | head -n-1)
echo "  HTTP $code6: $body6 (expected: null, newUserService has no ID=101)"
echo ""

# Test 7: GET /users/check/1 with tenantId=vip and version=v2 (should use newUserService)
echo "测试 7: GET /api/users/check/1 (tenantId=vip & version=v2 -> new service)"
result7=$(curl -s -w "\n%{http_code}" -H "tenantId: vip" -H "version: v2" "http://localhost:8084/api/users/check/1")
code7=$(echo "$result7" | tail -n1)
body7=$(echo "$result7" | head -n-1)
echo "  HTTP $code7: $body7"
echo ""

# Test 8: GET /users/check/1 with forceNew=true (should use newUserService)
echo "测试 8: GET /api/users/check/1?forceNew=true (-> new service)"
result8=$(curl -s -w "\n%{http_code}" "http://localhost:8084/api/users/check/1?forceNew=true")
code8=$(echo "$result8" | tail -n1)
body8=$(echo "$result8" | head -n-1)
echo "  HTTP $code8: $body8"
echo ""

# Test 9: POST /users without source header (should use oldUserService)
echo "测试 9: POST /api/users (无source -> old service)"
result9=$(curl -s -w "\n%{http_code}" -X POST -H "Content-Type: application/json" -d '{"name":"Test User 9","email":"test9@example.com"}' http://localhost:8084/api/users)
code9=$(echo "$result9" | tail -n1)
body9=$(echo "$result9" | head -n-1)
echo "  HTTP $code9: $body9"
echo ""

# Test 10: POST /users with source=third-party HEADER (should use newUserService)
echo "测试 10: POST /api/users (source=third-party header -> new service)"
result10=$(curl -s -w "\n%{http_code}" -X POST -H "Content-Type: application/json" -H "source: third-party" -d '{"name":"Test User 10","email":"test10@example.com"}' http://localhost:8084/api/users)
code10=$(echo "$result10" | tail -n1)
body10=$(echo "$result10" | head -n-1)
echo "  HTTP $code10: $body10"
echo ""

# Test 11: POST /users/register with username ending in -v2 (should use newUserService)
echo "测试 11: POST /api/users/register (username=test-v2 -> new service)"
result11=$(curl -s -w "\n%{http_code}" -X POST -H "Content-Type: application/json" -d '{"name":"Test V2","email":"testv2@example.com","username":"test-v2"}' http://localhost:8084/api/users/register)
code11=$(echo "$result11" | tail -n1)
body11=$(echo "$result11" | head -n-1)
echo "  HTTP $code11: $body11"
echo ""

# Test 12: GET /users/description without header (should use oldUserService)
echo "测试 12: GET /api/users/description (无请求头 -> old service)"
result12=$(curl -s -w "\n%{http_code}" http://localhost:8084/api/users/description)
code12=$(echo "$result12" | tail -n1)
body12=$(echo "$result12" | head -n-1)
echo "  HTTP $code12: $body12"
echo ""

# Test 13: GET /users/description with new=true HEADER (should use newUserService)
echo "测试 13: GET /api/users/description (new=true header -> new service)"
result13=$(curl -s -w "\n%{http_code}" -H "new: true" http://localhost:8084/api/users/description)
code13=$(echo "$result13" | tail -n1)
body13=$(echo "$result13" | head -n-1)
echo "  HTTP $code13: $body13"
echo ""

echo "========================================="
echo "测试完成！"
echo "========================================="
