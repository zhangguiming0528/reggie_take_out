package com.itheima.reggie.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.itheima.reggie.common.R;
import com.itheima.reggie.dto.DishDto;
import com.itheima.reggie.entity.Category;
import com.itheima.reggie.entity.Dish;
import com.itheima.reggie.entity.DishFlavor;
import com.itheima.reggie.entity.Setmeal;
import com.itheima.reggie.service.CategoryService;
import com.itheima.reggie.service.DishFlavorService;
import com.itheima.reggie.service.DishService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/dish")
@Slf4j
public class DishController {
    @Autowired
    private DishService dishService;
    @Autowired
    private DishFlavorService dishFlavorService;
    @Autowired
    private CategoryService categoryService;
    @Autowired
    private RedisTemplate redisTemplate;

    /**
     * 新增菜品
     * @param dishDto
     * @return
     */
    @PostMapping
    public R<String> save(@RequestBody DishDto dishDto){
        dishService.saveWithFlavor(dishDto);
        //清理某个分类下面的菜品缓存数据
        String key = "dish_" + dishDto.getCategoryId() + "_1";
        redisTemplate.delete(key);
        return R.success("新增菜品成功！");
    }

    /**
     * 菜品分页展示
     * @param page
     * @param pageSize
     * @param name
     * @return
     */
    @GetMapping("/page")
    public R<Page> page(int page,int pageSize,String name){
        //构造分页构造器对象
        Page<Dish> pageInfo = new Page<>(page,pageSize);
        Page<DishDto> dishDtoPage = new Page<>();

        //构造条件器
        LambdaQueryWrapper<Dish> queryWrapper = new LambdaQueryWrapper<>();
        //添加过滤条件
        queryWrapper.like(name!=null,Dish::getName,name);
        //添加排序条件
        queryWrapper.orderByDesc(Dish::getUpdateTime);
        //执行分页查询
        dishService.page(pageInfo,queryWrapper);
        //对象拷贝
        BeanUtils.copyProperties(pageInfo,dishDtoPage,"records");
        List<Dish> records = pageInfo.getRecords();
        List<DishDto> list = records.stream().map(item->{
            DishDto dishDto = new DishDto();
            BeanUtils.copyProperties(item,dishDto);
            Long categoryId = item.getCategoryId();//分类id
            //根据id查询分类对象
            Category category = categoryService.getById(categoryId);
            if(category!=null){
                String categoryName = category.getName();
                dishDto.setCategoryName(categoryName);
            }
            return dishDto;
        }).collect(Collectors.toList());
        dishDtoPage.setRecords(list);
        return R.success(dishDtoPage);
    }

    /**
     * 根据id查询菜品信息和对应的口味信息
     * @param id
     * @return
     */
    @GetMapping("/{id}")
    public R<DishDto> get (@PathVariable Long id){
        DishDto dishDto = dishService.getByIdWithFlavor(id);
        return R.success(dishDto);
    }

    /**
     * 修改菜品信息
     * @param dishDto
     * @return
     */
    @PutMapping
    public R<String> update(@RequestBody DishDto dishDto){
        dishService.updateWithFlavor(dishDto);
        //清理所有菜品的缓存数据
        Set keys = redisTemplate.keys("dish_*"); //获取所有以dish_xxx开头的key
        redisTemplate.delete(keys); //删除这些key
        return R.success("修改菜品信息成功!");
    }

    /**
     * 根据分类id或者菜品名称查询对应分类下的所有菜品
     * @param dish
     * @return
     */
    /*@GetMapping("/list")
    public R<List<Dish>> listByCategory(Dish dish){
        log.info("传入的参数为:{}",dish.toString());
        //获取传入的categoryId
        Long categoryId = dish.getCategoryId();
        //获取传入的菜品名称
        String dishName = dish.getName();
        //构建查询条件
        LambdaQueryWrapper<Dish> queryWrapper = new LambdaQueryWrapper<>();
        //根据分类查询
        queryWrapper.eq(categoryId!=null,Dish::getCategoryId,categoryId);
        //根据菜品名称模糊查询
        queryWrapper.like(StringUtils.isNotEmpty(dishName),Dish::getName,dishName);
        //只查询在售菜品
        queryWrapper.eq(Dish::getStatus,1);
        //构建倒序排序
        queryWrapper.orderByDesc(Dish::getUpdateTime);
        //执行查询得到集合
        List<Dish> list = dishService.list(queryWrapper);
        return R.success(list);
    }*/
    @GetMapping("/list")
    public R<List<DishDto>> listByCategory(Dish dish){
        log.info("传入的参数为:{}",dish.toString());

        List<DishDto> dishDtoList = null;
        //动态构造key
        String key = "dish_" + dish.getCategoryId() + "_" + dish.getStatus();
        //先从redis中获取缓存数据
        dishDtoList = (List<DishDto>) redisTemplate.opsForValue().get(key);
        if(dishDtoList != null){
            //如果存在，直接返回，无需查询数据库
            return R.success(dishDtoList);
        }
        //获取传入的categoryId
        Long categoryId = dish.getCategoryId();
        //获取传入的菜品名称
        String dishName = dish.getName();
        //构建查询条件
        LambdaQueryWrapper<Dish> queryWrapper = new LambdaQueryWrapper<>();
        //根据分类查询
        queryWrapper.eq(categoryId!=null,Dish::getCategoryId,categoryId);
        //根据菜品名称模糊查询
        queryWrapper.like(StringUtils.isNotEmpty(dishName),Dish::getName,dishName);
        //只查询在售菜品
        queryWrapper.eq(Dish::getStatus,1);
        //构建倒序排序
        queryWrapper.orderByDesc(Dish::getUpdateTime);
        //执行查询得到集合
        List<Dish> list = dishService.list(queryWrapper);
        dishDtoList = list.stream().map(item -> {
            DishDto dishDto = new DishDto();
            BeanUtils.copyProperties(item,dishDto);
            //根据id查询分类对象
            Category category = categoryService.getById(categoryId);
            if(category!=null){
                String categoryName = category.getName();
                dishDto.setCategoryName(categoryName);
            }
            //当前的菜品id
            Long dishId = dishDto.getId();
            LambdaQueryWrapper<DishFlavor> lambdaQueryWrapper = new LambdaQueryWrapper<>();
            lambdaQueryWrapper.eq(DishFlavor::getDishId,dishId);
            List<DishFlavor> dishFlavors = dishFlavorService.list(lambdaQueryWrapper);
            dishDto.setFlavors(dishFlavors);
            return dishDto;

        }).collect(Collectors.toList());
        //如果不存在，需要查询数据库，将查询到的菜品数据缓存到Redis
        redisTemplate.opsForValue().set(key,dishDtoList,60, TimeUnit.MINUTES);

        return R.success(dishDtoList);
    }

    /**
     * 批量启售和停售
     * @param status
     * @param ids
     * @return
     */
    @PostMapping("/status/{status}")
    public R<String> updateStatus(@PathVariable int status,@RequestParam List<Long> ids){
        //构造条件构造器
        LambdaUpdateWrapper<Dish> updateWrapper = new LambdaUpdateWrapper<>();
        //添加过滤条件
        updateWrapper.set(Dish::getStatus,status).in(Dish::getId,ids);
        dishService.update(updateWrapper);
        return R.success("批量操作成功!");
    }
}
