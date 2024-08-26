package io.dataease.font.server;

import io.dataease.api.font.api.FontApi;
import io.dataease.api.font.dto.FontDto;
import io.dataease.exception.DEException;
import jakarta.annotation.Resource;
import io.dataease.font.manage.FontManage;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/typeface")
public class FontServer implements FontApi {

    @Resource
    private FontManage fontManage;

    @Override
    public List<FontDto> list(FontDto fontDto) {
        return fontManage.list(fontDto);
    }

    @Override
    public FontDto create(FontDto fontDto) {
        return fontManage.create(fontDto);
    }

    @Override
    public FontDto edit(FontDto fontDto) {
        return fontManage.edit(fontDto);
    }

    @Override
    public void delete(Long id) {
        fontManage.delete(id);
    }

    @Override
    public void changeDefault(FontDto fontDto) {
        fontManage.changeDefault(fontDto);
    }

    @Override
    public void upload(MultipartFile file, long fontID) throws DEException {
        fontManage.upload(file, fontID);
    }
}
