package com.mycompany.webapp.controller;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Resource;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletResponse;

import org.springframework.util.FileCopyUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.mycompany.webapp.dto.Board;
import com.mycompany.webapp.dto.Pager;
import com.mycompany.webapp.service.BoardService;

import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/board")
@Slf4j
public class BoardController {
	@RequestMapping("/test")
	public Board test() {
		log.info("실행");
	    Board board = new Board();
	    board.setBno(1);
	    board.setBtitle("제목");
	    board.setBcontent("내용");
	    board.setMid("user");
	    board.setBdate(new Date());
	    return board;
	}
	
	@Resource
	private BoardService boardService;
	
	@GetMapping("/list")
	public Map<String,Object> list(@RequestParam(defaultValue = "1")int pageNo){
		log.info("실행");
		int totalRows = boardService.getTotalBoardNum();
		Pager pager = new Pager(5, 5, totalRows, pageNo);
		List<Board> list = boardService.getBoards(pager);
		
		Map<String,Object> map = new HashMap<>();
		map.put("boards",list);
		map.put("pager",pager);
		return  map;
	}
	
	@GetMapping("/{bno}")
	public Board read(@PathVariable int bno,@RequestParam(defaultValue = "false") boolean hit) {
		log.info("실행");
		Board board = boardService.getBoard(bno, hit);
		return board;
	}
	
	@PostMapping("/create")
	public Board create(Board board) throws IllegalStateException, IOException {
		log.info("실행");
		if(board.getBattach()!=null &&!board.getBattach().isEmpty()) {
			MultipartFile mf = board.getBattach();
			board.setBattachoname(mf.getOriginalFilename());
			board.setBattachsname(new Date().getTime()+ "-" + board.getBattachoname());
			board.setBattachtype(mf.getContentType());
			File file = new File("/Users/kimjungwoo/Documents/hyundai_it/upload_files/"+board.getBattachsname());
			mf.transferTo(file);
		}
		boardService.writeBoard(board);
		// 날짜에 대한 정보가 없기 때문에 DB에서 다시 읽어와야 
		board = boardService.getBoard(board.getBno(), false);
		return board;
	}
	
	//multipart/form-data로 데이터를 전송할 때는 PUT, PATCH 사용할 수 없다.
	@PostMapping("/update")
	public Board update(Board board) throws IllegalStateException, IOException {
		log.info("실행");
		if(board.getBattach()!=null &&!board.getBattach().isEmpty()) {
			MultipartFile mf = board.getBattach();
			board.setBattachoname(mf.getOriginalFilename());
			board.setBattachsname(new Date().getTime()+ "-" + board.getBattachoname());
			board.setBattachtype(mf.getContentType());
			File file = new File("/Users/kimjungwoo/Documents/hyundai_it/upload_files/"+board.getBattachsname());
			mf.transferTo(file);
		}
		boardService.updateBoard(board);
		// 날짜에 대한 정보가 없기 때문에 DB에서 다시 읽어와야 함
		board = boardService.getBoard(board.getBno(), false);
		return board;
	}
	
	@DeleteMapping("/{bno}")
	public Map<String,String> delete(@PathVariable int bno) {
		boardService.removeBoard(bno);
		Map<String,String> map = new HashMap<>();
		map.put("result","success");
		return map;
	}
	
	@GetMapping("/battach/{bno}")
	public void download(@PathVariable int bno, HttpServletResponse response) throws IOException {
		Board board = boardService.getBoard(bno,false);
		String battachoname = board.getBattachoname();
		if(battachoname==null)
			return;
		
		// 파일 이름이 한글로 되어있을 경우, 응답 헤더에 한글을 넣을 수 없기 때문에 변환해야 한다. 
		battachoname = new String(battachoname.getBytes("UTF-8"),"ISO-8859-1");
		String battachsname = board.getBattachsname();
		String battachtype = board.getBattachtype();
		
		//응답 생성
		response.setHeader("Content-Disposition", "attachment;filename=\""+battachoname+"\";");
		response.setContentType(battachtype);
		
		InputStream is = new FileInputStream("/Users/kimjungwoo/Documents/hyundai_it/upload_files/"+battachsname);
		OutputStream os = response.getOutputStream();
		FileCopyUtils.copy(is,os);
		os.flush();
		os.close();
	}
}
